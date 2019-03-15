package zebrostudio.wallr100.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat.JPEG
import android.graphics.BitmapFactory
import android.net.Uri
import com.zebrostudio.wallrcustoms.lowpoly.LowPoly
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import zebrostudio.wallr100.android.utils.compressBitmap
import zebrostudio.wallr100.android.utils.writeInputStreamUsingByteArray
import zebrostudio.wallr100.data.database.DatabaseHelper
import zebrostudio.wallr100.data.database.entity.CollectionDatabaseImageEntity
import zebrostudio.wallr100.data.exception.ImageDownloadException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

interface ImageHandler {
  fun isImageCached(link: String): Boolean
  fun fetchImage(link: String): Observable<Long>
  fun cancelFetchingImage()
  fun getImageBitmap(): Bitmap
  fun clearImageCache(): Completable
  fun getImageUri(): Uri
  fun convertUriToBitmap(uri: Uri): Single<Bitmap>
  fun convertImageInCacheToLowpoly(): Single<Bitmap>
  fun saveLowPolyImageToDownloads(): Completable
  fun saveImageToCollections(type: Int, details: String): Completable
}

const val BYTE_ARRAY_SIZE = 2048
const val DOWNLOAD_PROGRESS_COMPLETED_VALUE: Long = 100
const val READ_MODE = "r"
const val BITMAP_COMPRESS_QUALITY = 100
const val INITIAL_SIZE = 0
const val UID_AUTO_INCREMENT: Long = 0

class ImageHandlerImpl(
  private val context: Context,
  private val fileHandler: FileHandler,
  private val databaseHelper: DatabaseHelper
) : ImageHandler {

  internal var shouldContinueFetchingImage: Boolean = true
  private var imageCacheTracker: Pair<Boolean, String> = Pair(false, "")

  override fun isImageCached(link: String): Boolean {
    return (imageCacheTracker.first && (imageCacheTracker.second == link))
  }

  override fun fetchImage(link: String): Observable<Long> {
    return Observable.create {
      var connection: HttpURLConnection? = null
      var inputStream: InputStream? = null
      var outputStream: OutputStream? = null
      shouldContinueFetchingImage = true
      imageCacheTracker = Pair(false, "")
      try {
        connection = URL(link).openConnection() as HttpURLConnection
        connection.connect()
        val length = connection.contentLength
        if (length <= INITIAL_SIZE) {
          it.onError(ImageDownloadException())
        }
        inputStream = connection.inputStream
        outputStream = FileOutputStream(fileHandler.getCacheFile())
        val data = ByteArray(BYTE_ARRAY_SIZE)
        var count: Int = inputStream.read(data)
        var read: Long = 0
        while (count != -1) {
          read += count.toLong()
          outputStream.write(data, INITIAL_SIZE, count)
          if (shouldContinueFetchingImage) {
            val progress = (read * 100 / length)
            if (progress == DOWNLOAD_PROGRESS_COMPLETED_VALUE) {
              imageCacheTracker = Pair(true, link)
            }
            it.onNext(progress)
            count = inputStream.read(data)
          } else {
            connection.disconnect()
            outputStream.flush()
            outputStream.close()
            inputStream?.close()
            break
          }
        }
      } catch (e: IOException) {
        it.onError(e)
      } finally {
        connection?.disconnect()
        outputStream?.flush()
        outputStream?.close()
        inputStream?.close()
        it.onComplete()
      }
    }
  }

  override fun cancelFetchingImage() {
    shouldContinueFetchingImage = false
  }

  override fun getImageBitmap(): Bitmap {
    return BitmapFactory.Options().let {
      it.inPreferredConfig = Bitmap.Config.ARGB_8888
      BitmapFactory.decodeFile(fileHandler.getCacheFile().path, it)
    }
  }

  override fun clearImageCache(): Completable {
    return Completable.create {
      fileHandler.deleteCacheFiles()
      imageCacheTracker = Pair(false, "")
      it.onComplete()
    }
  }

  override fun getImageUri(): Uri {
    return getImageBitmap().let { bitmap ->
      fileHandler.getCacheFile().outputStream()
          .compressBitmap(bitmap, JPEG, BITMAP_COMPRESS_QUALITY)
      Uri.fromFile(fileHandler.getCacheFile())
    }
  }

  override fun convertUriToBitmap(uri: Uri): Single<Bitmap> {
    return Single.create {
      try {
        with(context.contentResolver.openFileDescriptor(uri, READ_MODE)) {
          BitmapFactory.decodeFileDescriptor(fileDescriptor).let { bitmap ->
            this?.close()
            fileHandler.getCacheFile().outputStream()
                .compressBitmap(bitmap, JPEG, BITMAP_COMPRESS_QUALITY)
            it.onSuccess(bitmap)
          }
        }
      } catch (exception: IOException) {
        it.onError(exception)
      }
    }
  }

  override fun convertImageInCacheToLowpoly(): Single<Bitmap> {
    return Single.create {
      LowPoly.generate(getImageBitmap()).let { bitmap ->
        try {
          fileHandler.getCacheFile().outputStream()
              .compressBitmap(bitmap, JPEG, BITMAP_COMPRESS_QUALITY)
          it.onSuccess(bitmap)
        } catch (exception: IOException) {
          it.onError(exception)
        }
      }
    }
  }

  override fun saveLowPolyImageToDownloads(): Completable {
    return Completable.create {
      try {
        fileHandler.getCacheFile().inputStream().let { inputStream ->
          fileHandler.getDownloadFile().outputStream()
              .writeInputStreamUsingByteArray(inputStream, BYTE_ARRAY_SIZE)
          inputStream.close()
          it.onComplete()
        }
      } catch (exception: IOException) {
        it.onError(exception)
      }
    }
  }

  override fun saveImageToCollections(type: Int, details: String): Completable {
    return Completable.create {
      try {
        fileHandler.getCacheFile().inputStream().let { inputStream ->
          fileHandler.getCollectionsFile().let { file ->
            file.outputStream()
                .writeInputStreamUsingByteArray(inputStream, BYTE_ARRAY_SIZE)
            databaseHelper.getDatabase().collectionsDao().insert(CollectionDatabaseImageEntity(
                UID_AUTO_INCREMENT,
                file.name,
                type,
                file.path,
                details
            ))
          }
          inputStream.close()
          it.onComplete()
        }
      } catch (exception: IOException) {
        it.onError(exception)
      }
    }
  }

}