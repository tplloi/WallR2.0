package zebrostudio.wallr100.presentation

import android.Manifest.*
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import com.uber.autodispose.lifecycle.TestLifecycleScopeProvider
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import zebrostudio.wallr100.R
import zebrostudio.wallr100.android.utils.WallpaperSetter
import zebrostudio.wallr100.domain.executor.PostExecutionThread
import zebrostudio.wallr100.domain.interactor.ImageOptionsUseCase
import zebrostudio.wallr100.domain.interactor.UserPremiumStatusUseCase
import zebrostudio.wallr100.domain.model.imagedownload.ImageDownloadModel
import zebrostudio.wallr100.presentation.adapters.ImageRecyclerViewPresenterImpl.ImageListType.*
import zebrostudio.wallr100.presentation.datafactory.ImagePresenterEntityFactory
import zebrostudio.wallr100.presentation.datafactory.SearchPicturesPresenterEntityFactory
import zebrostudio.wallr100.presentation.detail.ActionType.*
import zebrostudio.wallr100.presentation.detail.DetailContract
import zebrostudio.wallr100.presentation.detail.DetailPresenterImpl
import java.lang.Exception

@RunWith(MockitoJUnitRunner::class)
class DetailActivityPresenterImplTest {

  @Mock private lateinit var imageOptionsUseCase: ImageOptionsUseCase
  @Mock private lateinit var userPremiumStatusUseCase: UserPremiumStatusUseCase
  @Mock private lateinit var detailView: DetailContract.DetailView
  @Mock private lateinit var wallpaperSetter: WallpaperSetter
  @Mock private lateinit var dummyBitmap: Bitmap
  @Mock private lateinit var mockContext: Context
  @Mock private lateinit var postExecutionThread: PostExecutionThread
  private lateinit var detailPresenterImpl: DetailPresenterImpl
  private lateinit var testScopeProvider: TestLifecycleScopeProvider
  private val downloadProgressCompletedValue: Long = 100
  private val downloadProgressCompleteUpTo99: Long = 99
  private val downloadProgressCompleteUpTo98: Long = 98
  private val indefiniteLoaderWallpaperTypeMessage = "Finalizing wallpaper..."
  private val indefiniteLoaderSearchTypeMessage = "Firing up the editing tool..."

  @Before
  fun setup() {
    detailPresenterImpl =
        DetailPresenterImpl(mockContext, imageOptionsUseCase, userPremiumStatusUseCase,
            wallpaperSetter, postExecutionThread)
    detailPresenterImpl.attachView(detailView)

    testScopeProvider = TestLifecycleScopeProvider.createInitial(
        TestLifecycleScopeProvider.TestLifecycle.STARTED)
    `when`(detailView.getScope()).thenReturn(testScopeProvider)
    stubPostExecutionThreadReturnsIoScheduler()
  }

  @Test
  fun `should show search image details on setImageType as search call`() {
    val searchImagePresenterEntity =
        SearchPicturesPresenterEntityFactory.getSearchPicturesPresenterEntity()
    `when`(detailView.getSearchImageDetails()).thenReturn(searchImagePresenterEntity)

    detailPresenterImpl.setImageType(SEARCH)

    verify(detailView).getSearchImageDetails()
    verify(detailView).showAuthorDetails(searchImagePresenterEntity.userPresenterEntity.name,
        searchImagePresenterEntity.userPresenterEntity.profileImageLink)
    verify(detailView).showImage(
        searchImagePresenterEntity.imageQualityUrlPresenterEntity.smallImageLink,
        searchImagePresenterEntity.imageQualityUrlPresenterEntity.largeImageLink)
    verifyNoMoreInteractions(detailView)
  }

  @Test
  fun `should show wallpaper image details on setImageType as wallpaper call`() {
    val imagePresenterEntity = ImagePresenterEntityFactory.getImagePresenterEntity()
    `when`(detailView.getWallpaperImageDetails()).thenReturn(imagePresenterEntity)

    detailPresenterImpl.setImageType(WALLPAPERS)

    verify(detailView).getWallpaperImageDetails()
    verify(detailView).showAuthorDetails(imagePresenterEntity.author.name,
        imagePresenterEntity.author.profileImageLink)
    verify(detailView).showImage(imagePresenterEntity.imageLink.thumb,
        imagePresenterEntity.imageLink.large)
    verifyNoMoreInteractions(detailView)
  }

  @Test fun `should show error toast on high quality image loading failure`() {
    detailPresenterImpl.handleHighQualityImageLoadFailed()

    verify(detailView).showImageLoadError()
    verifyNoMoreInteractions(detailView)
  }

  @Test
  fun `should request storage permission on handleQuickSetClicked call`() {
    `when`(detailView.hasStoragePermission()).thenReturn(false)

    detailPresenterImpl.handleQuickSetClick()

    verify(detailView).hasStoragePermission()
    verify(detailView).requestStoragePermission(QUICK_SET)
    verifyNoMoreInteractions(detailView)
  }

  @Test
  fun `should show no internet error on handleQuickSetClicked call failure due to no internet`() {
    `when`(detailView.hasStoragePermission()).thenReturn(true)
    `when`(detailView.internetAvailability()).thenReturn(false)

    detailPresenterImpl.handleQuickSetClick()

    verify(detailView).hasStoragePermission()
    verify(detailView).internetAvailability()
    verify(detailView).showNoInternetError()
    verifyNoMoreInteractions(detailView)
  }

  @Test fun `should show image download progress on handleQuickSetClicked call success`() {
    val imageDownloadModel = ImageDownloadModel(downloadProgressCompleteUpTo98, null)
    detailPresenterImpl.imageType = WALLPAPERS
    detailPresenterImpl.wallpaperImage = ImagePresenterEntityFactory.getImagePresenterEntity()
    `when`(detailView.hasStoragePermission()).thenReturn(true)
    `when`(detailView.internetAvailability()).thenReturn(true)
    `when`(imageOptionsUseCase.fetchImageBitmapObservable(
        detailPresenterImpl.wallpaperImage.imageLink.large)).thenReturn(
        Observable.create {
          it.onNext(imageDownloadModel)
        })

    detailPresenterImpl.handleQuickSetClick()

    assertEquals(detailPresenterImpl.isDownloadInProgress, true)
    verify(detailView).hasStoragePermission()
    verify(detailView).internetAvailability()
    verify(detailView).blurScreenAndInitializeProgressPercentage()
    verify(detailView).getScope()
    verify(detailView).updateProgressPercentage("$downloadProgressCompleteUpTo98%")
    verifyNoMoreInteractions(detailView)
    verify(postExecutionThread).scheduler
    verifyNoMoreInteractions(postExecutionThread)
  }

  @Test
  fun `should show no internet error on handleShareClicked call failure due to no internet`() {
    `when`(detailView.internetAvailability()).thenReturn(false)

    detailPresenterImpl.handleShareClick()

    verify(detailView).internetAvailability()
    verify(detailView).showNoInternetToShareError()
    verifyNoMoreInteractions(detailView)
  }

  @Test fun `should redirect to pro when handleShareClicked call failure due to non pro user`() {
    `when`(detailView.internetAvailability()).thenReturn(true)
    `when`(userPremiumStatusUseCase.isUserPremium()).thenReturn(false)

    detailPresenterImpl.handleShareClick()

    verify(detailView).internetAvailability()
    verify(detailView).redirectToBuyPro(SHARE.ordinal)
    verifyNoMoreInteractions(detailView)
  }

  @Test
  fun `should show permission required message when handlePermissionRequestResult is called after permission is denied`() {
    detailPresenterImpl.handlePermissionRequestResult(QUICK_SET.ordinal,
        arrayOf(permission.READ_EXTERNAL_STORAGE, permission.WRITE_EXTERNAL_STORAGE),
        intArrayOf(PackageManager.PERMISSION_DENIED))

    verify(detailView).showPermissionRequiredMessage()
    verifyNoMoreInteractions(detailView)
  }

  @Test
  fun `should handle permission granted success and show progress after handlePermissionRequestResult is called`() {
    val imageDownloadModel = ImageDownloadModel(downloadProgressCompleteUpTo98, null)
    detailPresenterImpl.imageType = WALLPAPERS
    detailPresenterImpl.wallpaperImage = ImagePresenterEntityFactory.getImagePresenterEntity()
    `when`(imageOptionsUseCase.fetchImageBitmapObservable(
        detailPresenterImpl.wallpaperImage.imageLink.large)).thenReturn(
        Observable.create {
          it.onNext(imageDownloadModel)
        })

    detailPresenterImpl.handlePermissionRequestResult(QUICK_SET.ordinal,
        arrayOf(permission.READ_EXTERNAL_STORAGE, permission.WRITE_EXTERNAL_STORAGE),
        intArrayOf(PackageManager.PERMISSION_GRANTED))

    assertEquals(detailPresenterImpl.isDownloadInProgress, true)
    verify(detailView).blurScreenAndInitializeProgressPercentage()
    verify(detailView).getScope()
    verify(detailView).updateProgressPercentage("$downloadProgressCompleteUpTo98%")
    verifyNoMoreInteractions(detailView)
    shouldVerifyPostExecutionThreadSchedulerCall()
  }

  @Test
  fun `should handle permission granted success and show finalizing wallpaper message after handlePermissionRequestResult is called`() {
    val imageDownloadModel = ImageDownloadModel(downloadProgressCompleteUpTo99, null)
    detailPresenterImpl.imageType = WALLPAPERS
    detailPresenterImpl.wallpaperImage = ImagePresenterEntityFactory.getImagePresenterEntity()
    `when`(imageOptionsUseCase.fetchImageBitmapObservable(
        detailPresenterImpl.wallpaperImage.imageLink.large)).thenReturn(
        Observable.create {
          it.onNext(imageDownloadModel)
        })
    `when`(
        mockContext.getString(R.string.detail_activity_finalising_wallpaper_messsage)).thenReturn(
        indefiniteLoaderWallpaperTypeMessage)

    detailPresenterImpl.handlePermissionRequestResult(QUICK_SET.ordinal,
        arrayOf(permission.READ_EXTERNAL_STORAGE, permission.WRITE_EXTERNAL_STORAGE),
        intArrayOf(PackageManager.PERMISSION_GRANTED))

    assertEquals(detailPresenterImpl.isDownloadInProgress, false)
    assertEquals(detailPresenterImpl.isImageOperationInProgress, true)
    verify(detailView).blurScreenAndInitializeProgressPercentage()
    verify(detailView).getScope()
    verify(detailView).updateProgressPercentage("$downloadProgressCompletedValue%")
    verify(detailView).showIndefiniteLoaderWithAnimation(indefiniteLoaderWallpaperTypeMessage)
    verifyNoMoreInteractions(detailView)
    shouldVerifyPostExecutionThreadSchedulerCall()
  }

  @Test
  fun `should handle permission granted success and show firing up tool message after handlePermissionRequestResult is called`() {
    val imageDownloadModel = ImageDownloadModel(downloadProgressCompleteUpTo99, null)
    detailPresenterImpl.imageType = SEARCH
    detailPresenterImpl.searchImage =
        SearchPicturesPresenterEntityFactory.getSearchPicturesPresenterEntity()
    `when`(imageOptionsUseCase.fetchImageBitmapObservable(
        detailPresenterImpl.searchImage.imageQualityUrlPresenterEntity.largeImageLink)).thenReturn(
        Observable.create {
          it.onNext(imageDownloadModel)
        })
    `when`(mockContext.getString(R.string.detail_activity_editing_tool_message)).thenReturn(
        indefiniteLoaderSearchTypeMessage)

    detailPresenterImpl.handlePermissionRequestResult(QUICK_SET.ordinal,
        arrayOf(permission.READ_EXTERNAL_STORAGE, permission.WRITE_EXTERNAL_STORAGE),
        intArrayOf(PackageManager.PERMISSION_GRANTED))

    assertEquals(detailPresenterImpl.isDownloadInProgress, false)
    assertEquals(detailPresenterImpl.isImageOperationInProgress, true)
    verify(detailView).blurScreenAndInitializeProgressPercentage()
    verify(detailView).getScope()
    verify(detailView).updateProgressPercentage("$downloadProgressCompletedValue%")
    verify(detailView).showIndefiniteLoaderWithAnimation(indefiniteLoaderSearchTypeMessage)
    verifyNoMoreInteractions(detailView)
    shouldVerifyPostExecutionThreadSchedulerCall()
  }

  @Test
  fun `should handle permission granted success and set wallpaper successfully after handlePermissionRequestResult is called`() {
    val imageDownloadModel = ImageDownloadModel(downloadProgressCompletedValue, dummyBitmap)
    detailPresenterImpl.imageType = SEARCH
    detailPresenterImpl.searchImage =
        SearchPicturesPresenterEntityFactory.getSearchPicturesPresenterEntity()
    `when`(imageOptionsUseCase.fetchImageBitmapObservable(
        detailPresenterImpl.searchImage.imageQualityUrlPresenterEntity.largeImageLink)).thenReturn(
        Observable.just(imageDownloadModel))
    `when`(wallpaperSetter.setWallpaper(dummyBitmap)).thenReturn(true)

    detailPresenterImpl.handlePermissionRequestResult(QUICK_SET.ordinal,
        arrayOf(permission.READ_EXTERNAL_STORAGE, permission.WRITE_EXTERNAL_STORAGE),
        intArrayOf(PackageManager.PERMISSION_GRANTED))

    assertEquals(detailPresenterImpl.isImageOperationInProgress, false)
    verify(detailView).blurScreenAndInitializeProgressPercentage()
    verify(detailView).getScope()
    verify(detailView).showWallpaperSetSuccessMessage()
    verify(detailView).hideScreenBlur()
    verifyNoMoreInteractions(detailView)
    shouldVerifyPostExecutionThreadSchedulerCall()
  }

  @Test
  fun `should handle permission granted success show set wallpaper error message after handlePermissionRequestResult is called`() {
    val imageDownloadModel = ImageDownloadModel(downloadProgressCompletedValue, dummyBitmap)
    detailPresenterImpl.imageType = SEARCH
    detailPresenterImpl.searchImage =
        SearchPicturesPresenterEntityFactory.getSearchPicturesPresenterEntity()
    `when`(imageOptionsUseCase.fetchImageBitmapObservable(
        detailPresenterImpl.searchImage.imageQualityUrlPresenterEntity.largeImageLink)).thenReturn(
        Observable.just(imageDownloadModel))
    `when`(wallpaperSetter.setWallpaper(dummyBitmap)).thenReturn(false)

    detailPresenterImpl.handlePermissionRequestResult(QUICK_SET.ordinal,
        arrayOf(permission.READ_EXTERNAL_STORAGE, permission.WRITE_EXTERNAL_STORAGE),
        intArrayOf(PackageManager.PERMISSION_GRANTED))

    assertEquals(detailPresenterImpl.isImageOperationInProgress, false)
    verify(detailView).blurScreenAndInitializeProgressPercentage()
    verify(detailView).getScope()
    verify(detailView).showWallpaperSetErrorMessage()
    verify(detailView).hideScreenBlur()
    verifyNoMoreInteractions(detailView)
    shouldVerifyPostExecutionThreadSchedulerCall()
  }

  @Test fun `should cancel download on handleBackButtonClick call while download in progress`() {
    detailPresenterImpl.isDownloadInProgress = true

    detailPresenterImpl.handleBackButtonClick()

    assertEquals(detailPresenterImpl.isDownloadInProgress, false)
    verify(imageOptionsUseCase).cancelFetchImageOperation()
    verifyNoMoreInteractions(imageOptionsUseCase)
    verify(detailView).hideScreenBlur()
    verify(detailView).showDownloadWallpaperCancelledMessage()
    verifyNoMoreInteractions(detailView)
  }

  @Test
  fun `should cancel download when handleBackButtonClick is called while image downloading is in progress`() {
    detailPresenterImpl.isDownloadInProgress = true

    detailPresenterImpl.handleBackButtonClick()

    assertEquals(detailPresenterImpl.isDownloadInProgress, false)
    verify(imageOptionsUseCase).cancelFetchImageOperation()
    verifyNoMoreInteractions(imageOptionsUseCase)
    verify(detailView).hideScreenBlur()
    verify(detailView).showDownloadWallpaperCancelledMessage()
    verifyNoMoreInteractions(detailView)
  }

  @Test
  fun `should show wait message when handleBackButtonClick is called while image operation is in progress`() {
    detailPresenterImpl.isImageOperationInProgress = true

    detailPresenterImpl.handleBackButtonClick()

    verify(detailView).showWallpaperOperationInProgressWaitMessage()
    verifyNoMoreInteractions(detailView)
  }

  @Test
  fun `should exit activity when handleBackButtonClick is called and clearing cache is successful`() {
    detailPresenterImpl.isDownloadInProgress = false
    detailPresenterImpl.isImageOperationInProgress = false
    `when`(imageOptionsUseCase.clearCachesCompletable()).thenReturn(Completable.complete())

    detailPresenterImpl.handleBackButtonClick()

    verify(detailView).getScope()
    verify(detailView).exitView()
    verifyNoMoreInteractions(detailView)
  }

  @Test
  fun `should exit activity when handleBackButtonClick is called and clearing cache is unsuccessful`() {
    detailPresenterImpl.isDownloadInProgress = false
    detailPresenterImpl.isImageOperationInProgress = false
    `when`(imageOptionsUseCase.clearCachesCompletable()).thenReturn(Completable.error(Exception()))

    detailPresenterImpl.handleBackButtonClick()

    verify(detailView).getScope()
    verify(detailView).exitView()
    verifyNoMoreInteractions(detailView)
  }

  @After
  fun cleanup() {
    detailPresenterImpl.detachView()
  }

  private fun stubPostExecutionThreadReturnsIoScheduler() {
    whenever(postExecutionThread.scheduler).thenReturn(Schedulers.trampoline())
  }

  private fun shouldVerifyPostExecutionThreadSchedulerCall() {
    verify(postExecutionThread).scheduler
    verifyNoMoreInteractions(postExecutionThread)
  }

}