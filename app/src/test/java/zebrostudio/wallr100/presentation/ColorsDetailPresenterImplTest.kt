package zebrostudio.wallr100.presentation

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import com.uber.autodispose.lifecycle.TestLifecycleScopeProvider
import com.yalantis.ucrop.UCrop
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import zebrostudio.wallr100.R
import zebrostudio.wallr100.android.ui.buypro.PurchaseTransactionConfig
import zebrostudio.wallr100.android.ui.detail.colors.ColorsDetailMode.MULTIPLE
import zebrostudio.wallr100.android.ui.detail.colors.ColorsDetailMode.SINGLE
import zebrostudio.wallr100.android.utils.ResourceUtils
import zebrostudio.wallr100.android.utils.WallpaperSetter
import zebrostudio.wallr100.data.exception.AlreadyPresentInCollectionException
import zebrostudio.wallr100.domain.executor.PostExecutionThread
import zebrostudio.wallr100.domain.interactor.ColorImagesUseCase
import zebrostudio.wallr100.domain.interactor.UserPremiumStatusUseCase
import zebrostudio.wallr100.domain.model.CollectionsImageModel.EDITED
import zebrostudio.wallr100.domain.model.CollectionsImageModel.MINIMAL_COLOR
import zebrostudio.wallr100.presentation.detail.colors.ColorsActionType.ADD_TO_COLLECTION
import zebrostudio.wallr100.presentation.detail.colors.ColorsActionType.DOWNLOAD
import zebrostudio.wallr100.presentation.detail.colors.ColorsActionType.EDIT_SET
import zebrostudio.wallr100.presentation.detail.colors.ColorsActionType.LOAD_COLOR_WALLPAPER
import zebrostudio.wallr100.presentation.detail.colors.ColorsActionType.QUICK_SET
import zebrostudio.wallr100.presentation.detail.colors.ColorsActionType.SHARE
import zebrostudio.wallr100.presentation.detail.colors.ColorsDetailContract.ColorsDetailView
import zebrostudio.wallr100.presentation.detail.colors.ColorsDetailPresenterImpl
import zebrostudio.wallr100.presentation.minimal.MultiColorImageType.GRADIENT
import zebrostudio.wallr100.presentation.minimal.MultiColorImageType.MATERIAL
import zebrostudio.wallr100.presentation.minimal.MultiColorImageType.PLASMA
import java.util.UUID

@RunWith(MockitoJUnitRunner::class)
class ColorsDetailPresenterImplTest {

  @Mock lateinit var colorImagesUseCase: ColorImagesUseCase
  @Mock lateinit var userPremiumStatusUseCase: UserPremiumStatusUseCase
  @Mock lateinit var wallpaperSetter: WallpaperSetter
  @Mock lateinit var colorsDetailView: ColorsDetailView
  @Mock lateinit var postExecutionThread: PostExecutionThread
  @Mock lateinit var resourceUtils: ResourceUtils
  @Mock lateinit var mockBitmap: Bitmap
  @Mock lateinit var mockUri: Uri
  private lateinit var colorsDetailPresenterImpl: ColorsDetailPresenterImpl
  private val randomString = UUID.randomUUID().toString()
  private val randomInt = Math.random().toInt()

  @Before fun setup() {
    colorsDetailPresenterImpl =
        ColorsDetailPresenterImpl(resourceUtils, postExecutionThread, userPremiumStatusUseCase,
            colorImagesUseCase, wallpaperSetter)

    colorsDetailPresenterImpl.attachView(colorsDetailView)

    val testScopeProvider = TestLifecycleScopeProvider.createInitial(
        TestLifecycleScopeProvider.TestLifecycle.STARTED)
    `when`(colorsDetailView.getScope()).thenReturn(testScopeProvider)
    stubPostExecutionThreadReturnsIoScheduler()
  }

  @Test
  fun `should request storage permission on handleViewReadyState with type Single color call failure due to no permission`() {
    val list = listOf(randomString)
    `when`(colorsDetailView.hasStoragePermission()).thenReturn(false)
    colorsDetailPresenterImpl.setColorsDetailMode(SINGLE)
    colorsDetailPresenterImpl.setColorList(list)

    colorsDetailPresenterImpl.handleViewReadyState()

    assertEquals(MULTIPLE, colorsDetailPresenterImpl.colorsDetailMode)
    assertEquals(list, colorsDetailPresenterImpl.colorList)
    verify(colorsDetailView).hasStoragePermission()
    verify(colorsDetailView).requestStoragePermission(LOAD_COLOR_WALLPAPER)
  }

  @Test
  fun `should request storage permission on handleViewReadyState with type Material color call failure due to no permission`() {
    val list = listOf(randomString)
    `when`(colorsDetailView.getMultiColorImageType()).thenReturn(MATERIAL)
    `when`(colorsDetailView.hasStoragePermission()).thenReturn(false)
    colorsDetailPresenterImpl.setColorsDetailMode(MULTIPLE)
    colorsDetailPresenterImpl.setColorList(list)

    colorsDetailPresenterImpl.handleViewReadyState()

    assertEquals(MULTIPLE, colorsDetailPresenterImpl.colorsDetailMode)
    assertEquals(MATERIAL, colorsDetailPresenterImpl.multiColorImageType)
    assertEquals(list, colorsDetailPresenterImpl.colorList)
    verify(colorsDetailView).getMultiColorImageType()
    verify(colorsDetailView).hasStoragePermission()
    verify(colorsDetailView).requestStoragePermission(LOAD_COLOR_WALLPAPER)
  }

  @Test
  fun `should request storage permission on handleViewReadyState with type Gradient color call failure due to no permission`() {
    val list = listOf(randomString)
    `when`(colorsDetailView.getMultiColorImageType()).thenReturn(GRADIENT)
    colorsDetailPresenterImpl.setColorsDetailMode(MULTIPLE)
    colorsDetailPresenterImpl.setColorList(list)

    colorsDetailPresenterImpl.handleViewReadyState()

    assertEquals(MULTIPLE, colorsDetailPresenterImpl.colorsDetailMode)
    assertEquals(GRADIENT, colorsDetailPresenterImpl.multiColorImageType)
    assertEquals(list, colorsDetailPresenterImpl.colorList)
    verify(colorsDetailView).getMultiColorImageType()
    verify(colorsDetailView).hasStoragePermission()
    verify(colorsDetailView).requestStoragePermission(LOAD_COLOR_WALLPAPER)
    verifyNoMoreInteractions(colorsDetailView)
  }

  @Test
  fun `should request storage permission on handleViewReadyState with type Plasma color call failure due to no permission`() {
    val list = listOf(randomString)
    `when`(colorsDetailView.getMultiColorImageType()).thenReturn(PLASMA)
    colorsDetailPresenterImpl.setColorsDetailMode(MULTIPLE)
    colorsDetailPresenterImpl.setColorList(list)

    colorsDetailPresenterImpl.handleViewReadyState()

    assertEquals(MULTIPLE, colorsDetailPresenterImpl.colorsDetailMode)
    assertEquals(PLASMA, colorsDetailPresenterImpl.multiColorImageType)
    assertEquals(list, colorsDetailPresenterImpl.colorList)
    verify(colorsDetailView).getMultiColorImageType()
    verify(colorsDetailView).hasStoragePermission()
    verify(colorsDetailView).requestStoragePermission(LOAD_COLOR_WALLPAPER)
    verifyNoMoreInteractions(colorsDetailView)
  }

  @Test
  fun `should set text and show image on handleViewReadyState with type Single color call success`() {
    val list = listOf(randomString)
    `when`(colorsDetailView.hasStoragePermission()).thenReturn(true)
    `when`(resourceUtils.getStringResource(
        R.string.colors_detail_activity_colors_style_name_solid)).thenReturn(
        randomString)
    `when`(colorImagesUseCase.getSingularColorBitmapSingle(randomString)).thenReturn(
        Single.just(mockBitmap))
    colorsDetailPresenterImpl.setColorsDetailMode(SINGLE)
    colorsDetailPresenterImpl.setColorList(list)

    colorsDetailPresenterImpl.handleViewReadyState()

    assertEquals(SINGLE, colorsDetailPresenterImpl.colorsDetailMode)
    assertEquals(list, colorsDetailPresenterImpl.colorList)
    assertEquals(false, colorsDetailPresenterImpl.areColorOperationsDisabled)
    verify(colorsDetailView).getMultiColorImageType()
    verify(colorsDetailView).hasStoragePermission()
    verify(resourceUtils).getStringResource(R.string.colors_detail_activity_colors_style_name_solid)
    verifyNoMoreInteractions(resourceUtils)
    verifyImageShown()
    verifyPostExecutionThreadSchedulerCall()
  }

  @Test
  fun `should set text and show image load error on handleViewReadyState with type Single color call failure`() {
    val list = listOf(randomString)
    `when`(colorsDetailView.hasStoragePermission()).thenReturn(true)
    `when`(resourceUtils.getStringResource(
        R.string.colors_detail_activity_colors_style_name_solid)).thenReturn(
        randomString)
    `when`(colorImagesUseCase.getSingularColorBitmapSingle(randomString)).thenReturn(
        Single.error(Exception()))
    colorsDetailPresenterImpl.setColorsDetailMode(SINGLE)
    colorsDetailPresenterImpl.setColorList(list)

    colorsDetailPresenterImpl.handleViewReadyState()

    assertEquals(SINGLE, colorsDetailPresenterImpl.colorsDetailMode)
    assertEquals(list, colorsDetailPresenterImpl.colorList)
    assertEquals(false, colorsDetailPresenterImpl.areColorOperationsDisabled)
    verify(colorsDetailView).getMultiColorImageType()
    verify(colorsDetailView).hasStoragePermission()
    verify(resourceUtils).getStringResource(R.string.colors_detail_activity_colors_style_name_solid)
    verifyImageNotShown()
    verifyPostExecutionThreadSchedulerCall()
  }

  @Test
  fun `should set text and show image on handleViewReadyState with type Material color call success`() {
    val list = listOf(randomString)
    `when`(colorsDetailView.getMultiColorImageType()).thenReturn(MATERIAL)
    `when`(colorsDetailView.hasStoragePermission()).thenReturn(true)
    `when`(resourceUtils.getStringResource(
        R.string.colors_detail_activity_colors_style_name_material)).thenReturn(
        randomString)
    `when`(colorImagesUseCase.getMultiColorMaterialSingle(list,
        MATERIAL)).thenReturn(Single.just(mockBitmap))
    colorsDetailPresenterImpl.setColorsDetailMode(MULTIPLE)
    colorsDetailPresenterImpl.setColorList(list)

    colorsDetailPresenterImpl.handleViewReadyState()

    assertEquals(MULTIPLE, colorsDetailPresenterImpl.colorsDetailMode)
    assertEquals(MATERIAL, colorsDetailPresenterImpl.multiColorImageType)
    assertEquals(list, colorsDetailPresenterImpl.colorList)
    assertEquals(false, colorsDetailPresenterImpl.areColorOperationsDisabled)
    verify(colorsDetailView).getMultiColorImageType()
    verify(colorsDetailView).getMultiColorImageType()
    verify(colorsDetailView).hasStoragePermission()
    verify(resourceUtils).getStringResource(
        R.string.colors_detail_activity_colors_style_name_material)
    verifyImageShown()
    verifyPostExecutionThreadSchedulerCall()
  }

  @Test
  fun `should set text and show image load error on handleViewReadyState with type Material color call failure`() {
    val list = listOf(randomString)
    `when`(colorsDetailView.getMultiColorImageType()).thenReturn(MATERIAL)
    `when`(colorsDetailView.hasStoragePermission()).thenReturn(true)
    `when`(resourceUtils.getStringResource(
        R.string.colors_detail_activity_colors_style_name_material)).thenReturn(
        randomString)
    `when`(colorImagesUseCase.getMultiColorMaterialSingle(list, MATERIAL))
        .thenReturn(Single.error(Exception()))
    colorsDetailPresenterImpl.setColorsDetailMode(MULTIPLE)
    colorsDetailPresenterImpl.setColorList(list)

    colorsDetailPresenterImpl.handleViewReadyState()

    assertEquals(MULTIPLE, colorsDetailPresenterImpl.colorsDetailMode)
    assertEquals(MATERIAL, colorsDetailPresenterImpl.multiColorImageType)
    assertEquals(list, colorsDetailPresenterImpl.colorList)
    assertEquals(false, colorsDetailPresenterImpl.areColorOperationsDisabled)
    verify(colorsDetailView).getMultiColorImageType()
    verify(resourceUtils).getStringResource(
        R.string.colors_detail_activity_colors_style_name_material)
    verifyNoMoreInteractions(resourceUtils)
    verifyImageNotShown()
    verifyPostExecutionThreadSchedulerCall()
  }

  @Test
  fun `should set text and show image on handleViewReadyState with type Gradient color call success`() {
    val list = listOf(randomString)
    `when`(colorsDetailView.getMultiColorImageType()).thenReturn(GRADIENT)
    `when`(colorsDetailView.hasStoragePermission()).thenReturn(true)
    `when`(resourceUtils.getStringResource(
        R.string.colors_detail_activity_colors_style_name_gradient)).thenReturn(
        randomString)
    `when`(colorImagesUseCase.getMultiColorMaterialSingle(list, GRADIENT))
        .thenReturn(Single.just(mockBitmap))
    colorsDetailPresenterImpl.setColorsDetailMode(MULTIPLE)
    colorsDetailPresenterImpl.setColorList(list)

    colorsDetailPresenterImpl.handleViewReadyState()

    assertEquals(MULTIPLE, colorsDetailPresenterImpl.colorsDetailMode)
    assertEquals(GRADIENT, colorsDetailPresenterImpl.multiColorImageType)
    assertEquals(list, colorsDetailPresenterImpl.colorList)
    assertEquals(false, colorsDetailPresenterImpl.areColorOperationsDisabled)
    verify(colorsDetailView).getMultiColorImageType()
    verify(resourceUtils).getStringResource(
        R.string.colors_detail_activity_colors_style_name_gradient)
    verifyNoMoreInteractions(resourceUtils)
    verifyImageShown()
    verifyPostExecutionThreadSchedulerCall()
  }

  @Test
  fun `should set text and show image load error on handleViewReadyState with type Gradient color call failure`() {
    val list = listOf(randomString)
    `when`(colorsDetailView.getMultiColorImageType()).thenReturn(GRADIENT)
    `when`(colorsDetailView.hasStoragePermission()).thenReturn(true)
    `when`(
        resourceUtils.getStringResource(
            R.string.colors_detail_activity_colors_style_name_gradient)).thenReturn(
        randomString)
    `when`(colorImagesUseCase.getMultiColorMaterialSingle(list,
        GRADIENT)).thenReturn(Single.error(Exception()))
    colorsDetailPresenterImpl.setColorsDetailMode(MULTIPLE)
    colorsDetailPresenterImpl.setColorList(list)

    colorsDetailPresenterImpl.handleViewReadyState()

    assertEquals(MULTIPLE, colorsDetailPresenterImpl.colorsDetailMode)
    assertEquals(GRADIENT, colorsDetailPresenterImpl.multiColorImageType)
    assertEquals(list, colorsDetailPresenterImpl.colorList)
    assertEquals(false, colorsDetailPresenterImpl.areColorOperationsDisabled)
    verify(colorsDetailView).getMultiColorImageType()
    verify(resourceUtils).getStringResource(
        R.string.colors_detail_activity_colors_style_name_gradient)
    verifyImageNotShown()
    verifyPostExecutionThreadSchedulerCall()
  }

  @Test
  fun `should set text and show image on handleViewReadyState with type Plasma color call success`() {
    val list = listOf(randomString)
    `when`(colorsDetailView.getMultiColorImageType()).thenReturn(PLASMA)
    `when`(colorsDetailView.hasStoragePermission()).thenReturn(true)
    `when`(resourceUtils.getStringResource(
        R.string.colors_detail_activity_colors_style_name_plasma)).thenReturn(
        randomString)
    `when`(colorImagesUseCase.getMultiColorMaterialSingle(list,
        PLASMA)).thenReturn(Single.just(mockBitmap))
    colorsDetailPresenterImpl.setColorsDetailMode(MULTIPLE)
    colorsDetailPresenterImpl.setColorList(list)

    colorsDetailPresenterImpl.handleViewReadyState()

    assertEquals(MULTIPLE, colorsDetailPresenterImpl.colorsDetailMode)
    assertEquals(PLASMA, colorsDetailPresenterImpl.multiColorImageType)
    assertEquals(list, colorsDetailPresenterImpl.colorList)
    assertEquals(false, colorsDetailPresenterImpl.areColorOperationsDisabled)
    verify(colorsDetailView).getMultiColorImageType()
    verify(resourceUtils).getStringResource(
        R.string.colors_detail_activity_colors_style_name_plasma)
    verifyImageShown()
    verifyPostExecutionThreadSchedulerCall()
  }

  @Test
  fun `should set text and show image load error on setCalledIntent of type Plasma color call failure`() {
    val list = listOf(randomString)
    `when`(colorsDetailView.getMultiColorImageType()).thenReturn(PLASMA)
    `when`(colorsDetailView.hasStoragePermission()).thenReturn(true)
    `when`(resourceUtils.getStringResource(
        R.string.colors_detail_activity_colors_style_name_plasma)).thenReturn(
        randomString)
    `when`(colorImagesUseCase.getMultiColorMaterialSingle(list,
        PLASMA)).thenReturn(Single.error(Exception()))
    colorsDetailPresenterImpl.setColorsDetailMode(MULTIPLE)
    colorsDetailPresenterImpl.setColorList(list)

    colorsDetailPresenterImpl.handleViewReadyState()

    assertEquals(MULTIPLE, colorsDetailPresenterImpl.colorsDetailMode)
    assertEquals(PLASMA, colorsDetailPresenterImpl.multiColorImageType)
    assertEquals(list, colorsDetailPresenterImpl.colorList)
    assertEquals(false, colorsDetailPresenterImpl.areColorOperationsDisabled)
    verify(colorsDetailView).getMultiColorImageType()
    verify(resourceUtils).getStringResource(
        R.string.colors_detail_activity_colors_style_name_plasma)
    verifyImageNotShown()
    verifyPostExecutionThreadSchedulerCall()
  }

  @Test
  fun `should set text and show image on handlePermissionRequestResult call success with request code Load color wallpaper`() {
    val list = listOf(randomString)
    `when`(colorsDetailView.getMultiColorImageType()).thenReturn(MATERIAL)
    `when`(colorsDetailView.hasStoragePermission()).thenReturn(true)
    `when`(resourceUtils.getStringResource(
        R.string.colors_detail_activity_colors_style_name_material)).thenReturn(
        randomString)
    `when`(colorImagesUseCase.getMultiColorMaterialSingle(list,
        MATERIAL)).thenReturn(Single.just(mockBitmap))
    colorsDetailPresenterImpl.setColorsDetailMode(MULTIPLE)
    colorsDetailPresenterImpl.setColorList(list)

    colorsDetailPresenterImpl.handlePermissionRequestResult(LOAD_COLOR_WALLPAPER.ordinal,
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE),
        intArrayOf(PackageManager.PERMISSION_GRANTED))

    assertEquals(MULTIPLE, colorsDetailPresenterImpl.colorsDetailMode)
    assertEquals(MATERIAL, colorsDetailPresenterImpl.multiColorImageType)
    assertEquals(list, colorsDetailPresenterImpl.colorList)
    assertEquals(false, colorsDetailPresenterImpl.areColorOperationsDisabled)
    verify(colorsDetailView).getMultiColorImageType()
    verify(resourceUtils).getStringResource(
        R.string.colors_detail_activity_colors_style_name_material)
    verifyImageShown()
    verifyPostExecutionThreadSchedulerCall()
  }

  @Test
  fun `should show permission required and exit view on handlePermissionRequestResult call failure due to permission denied with load color wallpaper request code`() {
    colorsDetailPresenterImpl.handlePermissionRequestResult(LOAD_COLOR_WALLPAPER.ordinal,
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE),
        intArrayOf(PackageManager.PERMISSION_DENIED))

    verify(colorsDetailView).exitView()
    verify(colorsDetailView).showPermissionRequiredMessage()
    verifyNoMoreInteractions(colorsDetailView)
  }

  @Test fun `should set panel expanded to true on notifyPanelExpanded call success`() {
    colorsDetailPresenterImpl.notifyPanelExpanded()

    assertTrue(colorsDetailPresenterImpl.isPanelExpanded)
  }

  @Test fun `should set panel expanded to false on notifyPanelCollapsed call success`() {
    colorsDetailPresenterImpl.notifyPanelCollapsed()

    assertFalse(colorsDetailPresenterImpl.isPanelExpanded)
  }

  @Test
  fun `should collapse panel on handleImageViewClicked call success and panel was expanded`() {
    colorsDetailPresenterImpl.isPanelExpanded = true

    colorsDetailPresenterImpl.handleImageViewClicked()

    verify(colorsDetailView).collapsePanel()
    verifyNoMoreInteractions(colorsDetailView)
  }

  @Test
  fun `should show full screen image on handleImageViewClicked call success and panel was expanded`() {
    colorsDetailPresenterImpl.isPanelExpanded = false

    colorsDetailPresenterImpl.handleImageViewClicked()

    verify(colorsDetailView).showFullScreenImage()
    verifyNoMoreInteractions(colorsDetailView)
  }

  @Test
  fun `should show operation disabled message on handleQuickSetClick call failure due to image still being loaded`() {
    colorsDetailPresenterImpl.areColorOperationsDisabled = true

    colorsDetailPresenterImpl.handleQuickSetClick()

    verify(colorsDetailView).showColorOperationsDisabledMessage()
    verifyNoMoreInteractions(colorsDetailView)
  }

  @Test
  fun `should request storage permission on handleQuickSetClick call failure due to missing permission`() {
    colorsDetailPresenterImpl.areColorOperationsDisabled = false
    `when`(colorsDetailView.hasStoragePermission()).thenReturn(false)

    colorsDetailPresenterImpl.handleQuickSetClick()

    verify(colorsDetailView).hasStoragePermission()
    verify(colorsDetailView).requestStoragePermission(QUICK_SET)
    verifyNoMoreInteractions(colorsDetailView)
  }

  @Test
  fun `should show wallpaper set error message on handleQuickSetClick call failure`() {
    colorsDetailPresenterImpl.areColorOperationsDisabled = false
    `when`(colorsDetailView.hasStoragePermission()).thenReturn(true)
    `when`(colorImagesUseCase.getBitmapSingle()).thenReturn(Single.error(java.lang.Exception()))
    `when`(resourceUtils.getStringResource(R.string.finalizing_wallpaper_messsage)).thenReturn(
        randomString)

    colorsDetailPresenterImpl.handleQuickSetClick()

    assertFalse(colorsDetailPresenterImpl.isColorWallpaperOperationActive)
    verify(colorsDetailView).hasStoragePermission()
    verify(colorsDetailView).getScope()
    verify(colorsDetailView).showIndefiniteWaitLoader(randomString)
    verify(colorsDetailView).hideIndefiniteWaitLoader()
    verify(colorsDetailView).showWallpaperSetErrorMessage()
    verifyNoMoreInteractions(colorsDetailView)
    verifyPostExecutionThreadSchedulerCall()
  }

  @Test
  fun `should set wallpaper on handleQuickSetClick call success`() {
    colorsDetailPresenterImpl.areColorOperationsDisabled = false
    `when`(colorsDetailView.hasStoragePermission()).thenReturn(true)
    `when`(colorImagesUseCase.getBitmapSingle()).thenReturn(Single.just(mockBitmap))
    `when`(resourceUtils.getStringResource(R.string.finalizing_wallpaper_messsage)).thenReturn(
        randomString)

    colorsDetailPresenterImpl.handleQuickSetClick()

    assertFalse(colorsDetailPresenterImpl.isColorWallpaperOperationActive)
    verify(colorsDetailView).hasStoragePermission()
    verify(colorsDetailView).getScope()
    verify(colorsDetailView).showIndefiniteWaitLoader(randomString)
    verify(colorsDetailView).hideIndefiniteWaitLoader()
    verify(colorsDetailView).showWallpaperSetSuccessMessage()
    verifyNoMoreInteractions(colorsDetailView)
    verifyPostExecutionThreadSchedulerCall()
  }

  @Test
  fun `should set wallpaper on handlePermissionRequestResult call success with quick set request code`() {
    colorsDetailPresenterImpl.areColorOperationsDisabled = false
    `when`(colorsDetailView.hasStoragePermission()).thenReturn(true)
    `when`(colorImagesUseCase.getBitmapSingle()).thenReturn(Single.just(mockBitmap))
    `when`(resourceUtils.getStringResource(R.string.finalizing_wallpaper_messsage)).thenReturn(
        randomString)

    colorsDetailPresenterImpl.handlePermissionRequestResult(QUICK_SET.ordinal,
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE),
        intArrayOf(PackageManager.PERMISSION_GRANTED))

    assertFalse(colorsDetailPresenterImpl.isColorWallpaperOperationActive)
    verify(colorsDetailView).hasStoragePermission()
    verify(colorsDetailView).getScope()
    verify(colorsDetailView).showIndefiniteWaitLoader(randomString)
    verify(colorsDetailView).hideIndefiniteWaitLoader()
    verify(colorsDetailView).showWallpaperSetSuccessMessage()
    verifyNoMoreInteractions(colorsDetailView)
    verifyPostExecutionThreadSchedulerCall()
  }

  @Test
  fun `should show permission required on handlePermissionRequestResult call failure due to permission denied with quick set request code`() {
    colorsDetailPresenterImpl.handlePermissionRequestResult(QUICK_SET.ordinal,
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE),
        intArrayOf(PackageManager.PERMISSION_DENIED))

    verify(colorsDetailView).showPermissionRequiredMessage()
    verifyNoMoreInteractions(colorsDetailView)
  }

  @Test
  fun `should show operation disabled message on handleDownloadClick call failure due to image still being loaded`() {
    colorsDetailPresenterImpl.areColorOperationsDisabled = true

    colorsDetailPresenterImpl.handleDownloadClick()

    verify(colorsDetailView).showColorOperationsDisabledMessage()
    verifyNoMoreInteractions(colorsDetailView)
  }

  @Test
  fun `should redirect to buy pro on handleDownloadClick call failure due to non pro user`() {
    colorsDetailPresenterImpl.areColorOperationsDisabled = false
    `when`(userPremiumStatusUseCase.isUserPremium()).thenReturn(false)

    colorsDetailPresenterImpl.handleDownloadClick()

    verify(userPremiumStatusUseCase).isUserPremium()
    verifyNoMoreInteractions(userPremiumStatusUseCase)
    verify(colorsDetailView).redirectToBuyPro(DOWNLOAD.ordinal)
    verifyNoMoreInteractions(colorsDetailView)
  }

  @Test
  fun `should request storage permission on handleDownloadClick call failure due to missing permission`() {
    colorsDetailPresenterImpl.areColorOperationsDisabled = false
    `when`(userPremiumStatusUseCase.isUserPremium()).thenReturn(true)
    `when`(colorsDetailView.hasStoragePermission()).thenReturn(false)

    colorsDetailPresenterImpl.handleDownloadClick()


    verify(userPremiumStatusUseCase).isUserPremium()
    verifyNoMoreInteractions(userPremiumStatusUseCase)
    verify(colorsDetailView).hasStoragePermission()
    verify(colorsDetailView).requestStoragePermission(DOWNLOAD)
    verifyNoMoreInteractions(colorsDetailView)
  }

  @Test fun `should show error message on handleDownloadCLick call failure`() {
    colorsDetailPresenterImpl.areColorOperationsDisabled = false
    `when`(userPremiumStatusUseCase.isUserPremium()).thenReturn(true)
    `when`(colorsDetailView.hasStoragePermission()).thenReturn(true)
    `when`(colorImagesUseCase.downloadImage()).thenReturn(Completable.error(Exception()))
    `when`(resourceUtils.getStringResource(
        R.string.detail_activity_crystallizing_wallpaper_please_wait_message))
        .thenReturn(randomString)

    colorsDetailPresenterImpl.handleDownloadClick()

    assertFalse(colorsDetailPresenterImpl.isColorWallpaperOperationActive)
    verify(userPremiumStatusUseCase).isUserPremium()
    verifyNoMoreInteractions(userPremiumStatusUseCase)
    verify(colorsDetailView).hasStoragePermission()
    verify(colorsDetailView).showIndefiniteWaitLoader(randomString)
    verify(colorsDetailView).getScope()
    verify(colorsDetailView).hideIndefiniteWaitLoader()
    verify(colorsDetailView).showGenericErrorMessage()
    verifyNoMoreInteractions(colorsDetailView)
    verifyPostExecutionThreadSchedulerCall()
  }

  @Test
  fun `should download wallpaper on handlePermissionRequestResult call success with download request code`() {
    colorsDetailPresenterImpl.areColorOperationsDisabled = false
    `when`(userPremiumStatusUseCase.isUserPremium()).thenReturn(true)
    `when`(colorsDetailView.hasStoragePermission()).thenReturn(true)
    `when`(colorImagesUseCase.downloadImage()).thenReturn(Completable.error(Exception()))
    `when`(resourceUtils.getStringResource(
        R.string.detail_activity_crystallizing_wallpaper_please_wait_message))
        .thenReturn(randomString)

    colorsDetailPresenterImpl.handlePermissionRequestResult(DOWNLOAD.ordinal,
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE),
        intArrayOf(PackageManager.PERMISSION_GRANTED))

    assertFalse(colorsDetailPresenterImpl.isColorWallpaperOperationActive)
    verify(userPremiumStatusUseCase).isUserPremium()
    verifyNoMoreInteractions(userPremiumStatusUseCase)
    verify(colorsDetailView).hasStoragePermission()
    verify(colorsDetailView).showIndefiniteWaitLoader(randomString)
    verify(colorsDetailView).getScope()
    verify(colorsDetailView).hideIndefiniteWaitLoader()
    verify(colorsDetailView).showGenericErrorMessage()
    verifyNoMoreInteractions(colorsDetailView)
    verifyPostExecutionThreadSchedulerCall()
  }

  @Test
  fun `should show permission required on handlePermissionRequestResult call failure due to permission denied with download request code`() {
    colorsDetailPresenterImpl.handlePermissionRequestResult(DOWNLOAD.ordinal,
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE),
        intArrayOf(PackageManager.PERMISSION_DENIED))

    verify(colorsDetailView).showPermissionRequiredMessage()
    verifyNoMoreInteractions(colorsDetailView)
  }

  @Test
  fun `should show unsuccessful purchase error on handleViewResult call failure with download request code`() {
    colorsDetailPresenterImpl.handleViewResult(DOWNLOAD.ordinal, Activity.RESULT_CANCELED)

    verify(colorsDetailView).showUnsuccessfulPurchaseError()
  }

  @Test
  fun `should download wallpaper on handleViewResult call success with download request code`() {
    colorsDetailPresenterImpl.areColorOperationsDisabled = false
    `when`(userPremiumStatusUseCase.isUserPremium()).thenReturn(true)
    `when`(colorsDetailView.hasStoragePermission()).thenReturn(true)
    `when`(colorImagesUseCase.downloadImage()).thenReturn(Completable.error(Exception()))
    `when`(resourceUtils.getStringResource(
        R.string.detail_activity_crystallizing_wallpaper_please_wait_message))
        .thenReturn(randomString)

    colorsDetailPresenterImpl.handleViewResult(DOWNLOAD.ordinal,
        PurchaseTransactionConfig.PURCHASE_SUCCESSFUL_RESULT_CODE)

    assertFalse(colorsDetailPresenterImpl.isColorWallpaperOperationActive)
    verify(userPremiumStatusUseCase).isUserPremium()
    verifyNoMoreInteractions(userPremiumStatusUseCase)
    verify(colorsDetailView).hasStoragePermission()
    verify(colorsDetailView).showIndefiniteWaitLoader(randomString)
    verify(colorsDetailView).getScope()
    verify(colorsDetailView).hideIndefiniteWaitLoader()
    verify(colorsDetailView).showGenericErrorMessage()
    verifyNoMoreInteractions(colorsDetailView)
    verifyPostExecutionThreadSchedulerCall()
  }

  @Test
  fun `should show operation disabled message on handleEditSetClick call failure due to image still being loaded`() {
    colorsDetailPresenterImpl.areColorOperationsDisabled = true

    colorsDetailPresenterImpl.handleEditSetClick()

    verify(colorsDetailView).showColorOperationsDisabledMessage()
    verifyNoMoreInteractions(colorsDetailView)
  }

  @Test
  fun `should request storage permission on handleEditSetClick call failure due to missing permission`() {
    colorsDetailPresenterImpl.areColorOperationsDisabled = false
    `when`(colorsDetailView.hasStoragePermission()).thenReturn(false)

    colorsDetailPresenterImpl.handleEditSetClick()

    verify(colorsDetailView).hasStoragePermission()
    verify(colorsDetailView).requestStoragePermission(EDIT_SET)
    verifyNoMoreInteractions(colorsDetailView)
  }

  @Test fun `should start cropping activity on handleEditSetClick call success`() {
    colorsDetailPresenterImpl.areColorOperationsDisabled = false
    `when`(colorsDetailView.hasStoragePermission()).thenReturn(true)
    `when`(
        resourceUtils.getStringResource(R.string.detail_activity_editing_tool_message)).thenReturn(
        randomString)
    `when`(colorImagesUseCase.getCacheSourceUri()).thenReturn(mockUri)
    `when`(colorImagesUseCase.getCroppingDestinationUri()).thenReturn(mockUri)
    `when`(wallpaperSetter.getDesiredMinimumWidth()).thenReturn(randomInt)
    `when`(wallpaperSetter.getDesiredMinimumHeight()).thenReturn(randomInt)

    colorsDetailPresenterImpl.handleEditSetClick()

    assertFalse(colorsDetailPresenterImpl.isColorWallpaperOperationActive)
    verify(colorsDetailView).hasStoragePermission()
    verify(colorsDetailView).showIndefiniteWaitLoader(randomString)
    verify(colorsDetailView).startCroppingActivity(
        colorImagesUseCase.getCacheSourceUri(),
        colorImagesUseCase.getCroppingDestinationUri(),
        wallpaperSetter.getDesiredMinimumWidth(),
        wallpaperSetter.getDesiredMinimumHeight())
    verifyNoMoreInteractions(colorsDetailView)
  }

  @Test
  fun `should show permission required on handlePermissionRequestResult call failure due to permission denied with edit set request code`() {
    colorsDetailPresenterImpl.handlePermissionRequestResult(EDIT_SET.ordinal,
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE),
        intArrayOf(PackageManager.PERMISSION_DENIED))

    verify(colorsDetailView).showPermissionRequiredMessage()
  }

  @Test fun `should show error on handleViewResult call failure due to null crop uri`() {
    `when`(colorsDetailView.getUriFromResultIntent()).thenReturn(null)

    colorsDetailPresenterImpl.handleViewResult(UCrop.REQUEST_CROP, Activity.RESULT_OK)

    verify(colorsDetailView).getUriFromResultIntent()
    verify(colorsDetailView).hideIndefiniteWaitLoader()
    verify(colorsDetailView).showGenericErrorMessage()
  }

  @Test fun `should show error on handleViewResult call failure of request type crop`() {
    `when`(colorsDetailView.getUriFromResultIntent()).thenReturn(mockUri)
    `when`(resourceUtils.getStringResource(R.string.finalizing_wallpaper_messsage)).thenReturn(
        randomString)
    `when`(colorImagesUseCase.getBitmapFromUriSingle(mockUri)).thenReturn(Single.error(Exception()))

    colorsDetailPresenterImpl.handleViewResult(UCrop.REQUEST_CROP, Activity.RESULT_OK)

    assertFalse(colorsDetailPresenterImpl.isColorWallpaperOperationActive)
    verify(colorsDetailView).getUriFromResultIntent()
    verify(colorsDetailView).getScope()
    verify(colorsDetailView).showIndefiniteWaitLoader(randomString)
    verify(colorsDetailView).hideIndefiniteWaitLoader()
    verify(colorsDetailView).showGenericErrorMessage()
  }

  @Test
  fun `should show unable to set wallpaper error on handleViewResult call success of request type crop`() {
    `when`(colorsDetailView.getUriFromResultIntent()).thenReturn(mockUri)
    `when`(resourceUtils.getStringResource(R.string.finalizing_wallpaper_messsage)).thenReturn(
        randomString)
    `when`(colorImagesUseCase.getBitmapFromUriSingle(mockUri)).thenReturn(Single.just(mockBitmap))
    `when`(wallpaperSetter.setWallpaper(mockBitmap)).thenReturn(false)

    colorsDetailPresenterImpl.handleViewResult(UCrop.REQUEST_CROP, Activity.RESULT_OK)

    assertFalse(colorsDetailPresenterImpl.isColorWallpaperOperationActive)
    assertEquals(EDITED, colorsDetailPresenterImpl.lastImageOperationType)
    verify(colorsDetailView).getUriFromResultIntent()
    verify(colorsDetailView).getScope()
    verify(colorsDetailView).showIndefiniteWaitLoader(randomString)
    verify(colorsDetailView).showImage(mockBitmap)
    verify(colorsDetailView).hideIndefiniteWaitLoader()
    verify(colorsDetailView).showWallpaperSetErrorMessage()
    verify(wallpaperSetter).setWallpaper(mockBitmap)
    verifyPostExecutionThreadSchedulerCall()
  }

  @Test fun `should set wallpaper on handleViewResult call success of request type crop`() {
    `when`(colorsDetailView.getUriFromResultIntent()).thenReturn(mockUri)
    `when`(resourceUtils.getStringResource(R.string.finalizing_wallpaper_messsage)).thenReturn(
        randomString)
    `when`(colorImagesUseCase.getBitmapFromUriSingle(mockUri)).thenReturn(Single.just(mockBitmap))
    `when`(wallpaperSetter.setWallpaper(mockBitmap)).thenReturn(true)

    colorsDetailPresenterImpl.handleViewResult(UCrop.REQUEST_CROP, Activity.RESULT_OK)

    assertFalse(colorsDetailPresenterImpl.isColorWallpaperOperationActive)
    assertEquals(EDITED, colorsDetailPresenterImpl.lastImageOperationType)
    verify(colorsDetailView).getUriFromResultIntent()
    verify(colorsDetailView).getScope()
    verify(colorsDetailView).showIndefiniteWaitLoader(randomString)
    verify(colorsDetailView).showImage(mockBitmap)
    verify(colorsDetailView).hideIndefiniteWaitLoader()
    verify(colorsDetailView).showWallpaperSetSuccessMessage()
    verify(wallpaperSetter).setWallpaper(mockBitmap)
    verifyPostExecutionThreadSchedulerCall()
  }

  @Test
  fun `should show operation disabled message on handleAddToCollectionClick call failure due to image still being loaded`() {
    colorsDetailPresenterImpl.areColorOperationsDisabled = true

    colorsDetailPresenterImpl.handleAddToCollectionClick()

    verify(colorsDetailView).showColorOperationsDisabledMessage()
  }

  @Test
  fun `should redirect to buy pro on handleAddToCollectionClick call failure due to non pro user`() {
    colorsDetailPresenterImpl.areColorOperationsDisabled = false
    `when`(userPremiumStatusUseCase.isUserPremium()).thenReturn(false)

    colorsDetailPresenterImpl.handleAddToCollectionClick()

    verify(userPremiumStatusUseCase).isUserPremium()
    verify(colorsDetailView).redirectToBuyPro(ADD_TO_COLLECTION.ordinal)
  }

  @Test
  fun `should request storage permission on handleAddToCollectionClick call failure due to missing permission`() {
    colorsDetailPresenterImpl.areColorOperationsDisabled = false
    `when`(userPremiumStatusUseCase.isUserPremium()).thenReturn(true)
    `when`(colorsDetailView.hasStoragePermission()).thenReturn(false)

    colorsDetailPresenterImpl.handleAddToCollectionClick()


    verify(userPremiumStatusUseCase).isUserPremium()
    verify(colorsDetailView).hasStoragePermission()
    verify(colorsDetailView).requestStoragePermission(ADD_TO_COLLECTION)
  }

  @Test
  fun `should show generic error on handleAddToCollectionClick call failure`() {
    val list = mutableListOf(randomString)
    colorsDetailPresenterImpl.colorList = list
    colorsDetailPresenterImpl.lastImageOperationType = MINIMAL_COLOR
    colorsDetailPresenterImpl.areColorOperationsDisabled = false
    `when`(userPremiumStatusUseCase.isUserPremium()).thenReturn(true)
    `when`(colorsDetailView.hasStoragePermission()).thenReturn(true)
    `when`(colorImagesUseCase.saveToCollectionsCompletable(list.toString(),
        MINIMAL_COLOR)).thenReturn(
        Completable.error(Exception()))
    `when`(
        resourceUtils.getStringResource(R.string.adding_image_to_collections_message)).thenReturn(
        randomString)

    colorsDetailPresenterImpl.handleAddToCollectionClick()

    assertFalse(colorsDetailPresenterImpl.isColorWallpaperOperationActive)
    verify(userPremiumStatusUseCase).isUserPremium()
    verify(colorsDetailView).hasStoragePermission()
    verify(colorsDetailView).getScope()
    verify(colorsDetailView).showIndefiniteWaitLoader(randomString)
    verify(colorsDetailView).showGenericErrorMessage()
    verify(colorsDetailView).hideIndefiniteWaitLoader()
    verifyPostExecutionThreadSchedulerCall()
  }

  @Test
  fun `should show already present error in collection message on handleAddToCollectionClick call failure`() {
    val list = mutableListOf(randomString)
    colorsDetailPresenterImpl.colorList = list
    colorsDetailPresenterImpl.lastImageOperationType = MINIMAL_COLOR
    colorsDetailPresenterImpl.areColorOperationsDisabled = false
    `when`(userPremiumStatusUseCase.isUserPremium()).thenReturn(true)
    `when`(colorsDetailView.hasStoragePermission()).thenReturn(true)
    `when`(colorImagesUseCase.saveToCollectionsCompletable(list.toString(),
        MINIMAL_COLOR)).thenReturn(
        Completable.error(AlreadyPresentInCollectionException()))
    `when`(
        resourceUtils.getStringResource(R.string.adding_image_to_collections_message)).thenReturn(
        randomString)

    colorsDetailPresenterImpl.handleAddToCollectionClick()

    assertFalse(colorsDetailPresenterImpl.isColorWallpaperOperationActive)
    verify(userPremiumStatusUseCase).isUserPremium()
    verify(colorsDetailView).hasStoragePermission()
    verify(colorsDetailView).getScope()
    verify(colorsDetailView).showIndefiniteWaitLoader(randomString)
    verify(colorsDetailView).showAlreadyPresentInCollectionErrorMessage()
    verify(colorsDetailView).hideIndefiniteWaitLoader()
    verifyPostExecutionThreadSchedulerCall()
  }

  @Test
  fun `should add to collection on handleAddToCollectionClick call success`() {
    val list = mutableListOf(randomString)
    colorsDetailPresenterImpl.colorList = list
    colorsDetailPresenterImpl.lastImageOperationType = MINIMAL_COLOR
    colorsDetailPresenterImpl.areColorOperationsDisabled = false
    `when`(userPremiumStatusUseCase.isUserPremium()).thenReturn(true)
    `when`(colorsDetailView.hasStoragePermission()).thenReturn(true)
    `when`(colorImagesUseCase.saveToCollectionsCompletable(list.toString(),
        MINIMAL_COLOR)).thenReturn(
        Completable.complete())
    `when`(
        resourceUtils.getStringResource(R.string.adding_image_to_collections_message)).thenReturn(
        randomString)

    colorsDetailPresenterImpl.handleAddToCollectionClick()

    assertFalse(colorsDetailPresenterImpl.isColorWallpaperOperationActive)
    verify(userPremiumStatusUseCase).isUserPremium()
    verify(colorsDetailView).hasStoragePermission()
    verify(colorsDetailView).getScope()
    verify(colorsDetailView).showIndefiniteWaitLoader(randomString)
    verify(colorsDetailView).showAddToCollectionSuccessMessage()
    verify(colorsDetailView).hideIndefiniteWaitLoader()
    verifyPostExecutionThreadSchedulerCall()
  }

  @Test
  fun `should show unsuccessful purchase error on handleViewResult call failure with add to collection request codee`() {
    colorsDetailPresenterImpl.handleViewResult(ADD_TO_COLLECTION.ordinal, Activity.RESULT_CANCELED)

    verify(colorsDetailView).showUnsuccessfulPurchaseError()
  }

  @Test
  fun `should add to collection on handleViewResult call success with add to collection request code`() {
    val list = mutableListOf(randomString)
    colorsDetailPresenterImpl.colorList = list
    colorsDetailPresenterImpl.lastImageOperationType = MINIMAL_COLOR
    colorsDetailPresenterImpl.areColorOperationsDisabled = false
    `when`(userPremiumStatusUseCase.isUserPremium()).thenReturn(true)
    `when`(colorsDetailView.hasStoragePermission()).thenReturn(true)
    `when`(colorImagesUseCase.saveToCollectionsCompletable(list.toString(),
        MINIMAL_COLOR)).thenReturn(
        Completable.complete())
    `when`(
        resourceUtils.getStringResource(R.string.adding_image_to_collections_message)).thenReturn(
        randomString)

    colorsDetailPresenterImpl.handleViewResult(ADD_TO_COLLECTION.ordinal,
        PurchaseTransactionConfig.PURCHASE_SUCCESSFUL_RESULT_CODE)

    assertFalse(colorsDetailPresenterImpl.isColorWallpaperOperationActive)
    verify(userPremiumStatusUseCase).isUserPremium()
    verifyNoMoreInteractions(userPremiumStatusUseCase)
    verify(colorsDetailView).hasStoragePermission()
    verify(colorsDetailView).getScope()
    verify(colorsDetailView).showIndefiniteWaitLoader(randomString)
    verify(colorsDetailView).showAddToCollectionSuccessMessage()
    verify(colorsDetailView).hideIndefiniteWaitLoader()
    verifyPostExecutionThreadSchedulerCall()
  }

  @Test
  fun `should add to collection on handlePermissionRequestResult call success with add to collection request code`() {
    val list = mutableListOf(randomString)
    colorsDetailPresenterImpl.colorList = list
    colorsDetailPresenterImpl.lastImageOperationType = MINIMAL_COLOR
    colorsDetailPresenterImpl.areColorOperationsDisabled = false
    `when`(userPremiumStatusUseCase.isUserPremium()).thenReturn(true)
    `when`(colorsDetailView.hasStoragePermission()).thenReturn(true)
    `when`(colorImagesUseCase.saveToCollectionsCompletable(list.toString(),
        MINIMAL_COLOR)).thenReturn(
        Completable.complete())
    `when`(
        resourceUtils.getStringResource(R.string.adding_image_to_collections_message)).thenReturn(
        randomString)

    colorsDetailPresenterImpl.handlePermissionRequestResult(ADD_TO_COLLECTION.ordinal,
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE),
        intArrayOf(PackageManager.PERMISSION_GRANTED))

    assertFalse(colorsDetailPresenterImpl.isColorWallpaperOperationActive)
    verify(userPremiumStatusUseCase).isUserPremium()
    verifyNoMoreInteractions(userPremiumStatusUseCase)
    verify(colorsDetailView).hasStoragePermission()
    verify(colorsDetailView).getScope()
    verify(colorsDetailView).showIndefiniteWaitLoader(randomString)
    verify(colorsDetailView).showAddToCollectionSuccessMessage()
    verify(colorsDetailView).hideIndefiniteWaitLoader()
    verifyPostExecutionThreadSchedulerCall()
  }

  @Test
  fun `should show permission required on handlePermissionRequestResult call failure due to permission denied with add to collection request code`() {
    colorsDetailPresenterImpl.handlePermissionRequestResult(ADD_TO_COLLECTION.ordinal,
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE),
        intArrayOf(PackageManager.PERMISSION_DENIED))

    verify(colorsDetailView).showPermissionRequiredMessage()
  }

  @Test
  fun `should show operation disabled message on handleShareClick call failure due to image still being loaded`() {
    colorsDetailPresenterImpl.areColorOperationsDisabled = true

    colorsDetailPresenterImpl.handleShareClick()

    verify(colorsDetailView).showColorOperationsDisabledMessage()
  }

  @Test
  fun `should redirect to buy pro on handleShareClick call failure due to non pro user`() {
    colorsDetailPresenterImpl.areColorOperationsDisabled = false
    `when`(userPremiumStatusUseCase.isUserPremium()).thenReturn(false)

    colorsDetailPresenterImpl.handleShareClick()

    verify(userPremiumStatusUseCase).isUserPremium()
    verifyNoMoreInteractions(userPremiumStatusUseCase)
    verify(colorsDetailView).redirectToBuyPro(SHARE.ordinal)
  }

  @Test
  fun `should request storage permission on handleShareClick call failure due to missing permission`() {
    colorsDetailPresenterImpl.areColorOperationsDisabled = false
    `when`(userPremiumStatusUseCase.isUserPremium()).thenReturn(true)
    `when`(colorsDetailView.hasStoragePermission()).thenReturn(false)

    colorsDetailPresenterImpl.handleShareClick()


    verify(userPremiumStatusUseCase).isUserPremium()
    verifyNoMoreInteractions(userPremiumStatusUseCase)
    verify(colorsDetailView).hasStoragePermission()
    verify(colorsDetailView).requestStoragePermission(SHARE)
  }

  @Test fun `should show generic error on handleShareClick call failure`() {
    colorsDetailPresenterImpl.areColorOperationsDisabled = false
    `when`(userPremiumStatusUseCase.isUserPremium()).thenReturn(true)
    `when`(colorsDetailView.hasStoragePermission()).thenReturn(true)
    `when`(colorImagesUseCase.getCacheImageUri()).thenReturn(Single.error(Exception()))
    `when`(
        resourceUtils.getStringResource(R.string.preparing_shareable_wallpaper_message)).thenReturn(
        randomString)

    colorsDetailPresenterImpl.handleShareClick()

    assertFalse(colorsDetailPresenterImpl.isColorWallpaperOperationActive)
    verify(userPremiumStatusUseCase).isUserPremium()
    verifyNoMoreInteractions(userPremiumStatusUseCase)
    verify(colorsDetailView).hasStoragePermission()
    verify(colorsDetailView).getScope()
    verify(colorsDetailView).showIndefiniteWaitLoader(randomString)
    verify(colorsDetailView).hideIndefiniteWaitLoader()
    verify(colorsDetailView).showGenericErrorMessage()
    verifyPostExecutionThreadSchedulerCall()
  }

  @Test fun `should share image on handleShareClick call success`() {
    colorsDetailPresenterImpl.areColorOperationsDisabled = false
    `when`(userPremiumStatusUseCase.isUserPremium()).thenReturn(true)
    `when`(colorsDetailView.hasStoragePermission()).thenReturn(true)
    `when`(colorImagesUseCase.getCacheImageUri()).thenReturn(Single.just(mockUri))
    `when`(
        resourceUtils.getStringResource(R.string.preparing_shareable_wallpaper_message)).thenReturn(
        randomString)

    colorsDetailPresenterImpl.handleShareClick()

    assertFalse(colorsDetailPresenterImpl.isColorWallpaperOperationActive)
    verify(userPremiumStatusUseCase).isUserPremium()
    verifyNoMoreInteractions(userPremiumStatusUseCase)
    verify(colorsDetailView).hasStoragePermission()
    verify(colorsDetailView).getScope()
    verify(colorsDetailView).showIndefiniteWaitLoader(randomString)
    verify(colorsDetailView).showShareIntent(mockUri)
    verify(colorsDetailView).hideIndefiniteWaitLoader()
    verifyPostExecutionThreadSchedulerCall()
  }

  @Test
  fun `should show unsuccessful purchase error on handleViewResult call failure with share request codee`() {
    colorsDetailPresenterImpl.handleViewResult(SHARE.ordinal, Activity.RESULT_CANCELED)

    verify(colorsDetailView).showUnsuccessfulPurchaseError()
    verifyNoMoreInteractions(colorsDetailView)
  }

  @Test
  fun `should add to collection on handleViewResult call success with add to share request code`() {
    colorsDetailPresenterImpl.areColorOperationsDisabled = false
    `when`(userPremiumStatusUseCase.isUserPremium()).thenReturn(true)
    `when`(colorsDetailView.hasStoragePermission()).thenReturn(true)
    `when`(colorImagesUseCase.getCacheImageUri()).thenReturn(Single.just(mockUri))
    `when`(
        resourceUtils.getStringResource(R.string.preparing_shareable_wallpaper_message)).thenReturn(
        randomString)

    colorsDetailPresenterImpl.handleViewResult(SHARE.ordinal,
        PurchaseTransactionConfig.PURCHASE_SUCCESSFUL_RESULT_CODE)

    assertFalse(colorsDetailPresenterImpl.isColorWallpaperOperationActive)
    verify(userPremiumStatusUseCase).isUserPremium()
    verifyNoMoreInteractions(userPremiumStatusUseCase)
    verify(colorsDetailView).hasStoragePermission()
    verify(colorsDetailView).getScope()
    verify(colorsDetailView).showIndefiniteWaitLoader(randomString)
    verify(colorsDetailView).showShareIntent(mockUri)
    verify(colorsDetailView).hideIndefiniteWaitLoader()
    verifyPostExecutionThreadSchedulerCall()
  }

  @Test
  fun `should add to collection on handlePermissionRequestResult call success with share request code`() {
    colorsDetailPresenterImpl.areColorOperationsDisabled = false
    `when`(userPremiumStatusUseCase.isUserPremium()).thenReturn(true)
    `when`(colorsDetailView.hasStoragePermission()).thenReturn(true)
    `when`(colorImagesUseCase.getCacheImageUri()).thenReturn(Single.just(mockUri))
    `when`(
        resourceUtils.getStringResource(R.string.preparing_shareable_wallpaper_message)).thenReturn(
        randomString)

    colorsDetailPresenterImpl.handlePermissionRequestResult(SHARE.ordinal,
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE),
        intArrayOf(PackageManager.PERMISSION_GRANTED))

    assertFalse(colorsDetailPresenterImpl.isColorWallpaperOperationActive)
    verify(userPremiumStatusUseCase).isUserPremium()
    verify(colorsDetailView).hasStoragePermission()
    verify(colorsDetailView).getScope()
    verify(colorsDetailView).showIndefiniteWaitLoader(randomString)
    verify(colorsDetailView).showShareIntent(mockUri)
    verify(colorsDetailView).hideIndefiniteWaitLoader()
    verifyPostExecutionThreadSchedulerCall()
  }

  @Test
  fun `should show permission required on handlePermissionRequestResult call failure due to permission denied with share request code`() {
    colorsDetailPresenterImpl.handlePermissionRequestResult(SHARE.ordinal,
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE),
        intArrayOf(PackageManager.PERMISSION_DENIED))

    verify(colorsDetailView).showPermissionRequiredMessage()
  }

  @After fun tearDown() {
    verifyNoMoreInteractions(postExecutionThread, wallpaperSetter, userPremiumStatusUseCase,
        colorImagesUseCase, colorsDetailView, resourceUtils, mockUri, mockBitmap)
    colorsDetailPresenterImpl.detachView()
  }

  private fun stubPostExecutionThreadReturnsIoScheduler() {
    whenever(postExecutionThread.scheduler).thenReturn(Schedulers.trampoline())
  }

  private fun verifyPostExecutionThreadSchedulerCall(times: Int = 1) {
    verify(postExecutionThread, times(times)).scheduler
  }

  private fun verifyImageShown() {
    verify(colorsDetailView).getScope()
    verify(colorsDetailView).hasStoragePermission()
    verify(colorsDetailView).showImageTypeText(randomString)
    verify(colorsDetailView).disableColorOperations()
    verify(colorsDetailView).showMainImageWaitLoader()
    verify(colorsDetailView).showImage(mockBitmap)
    verify(colorsDetailView).hideMainImageWaitLoader()
    verify(colorsDetailView).enableColorOperations()
  }

  private fun verifyImageNotShown() {
    verify(colorsDetailView).getScope()
    verify(colorsDetailView).hasStoragePermission()
    verify(colorsDetailView).showImageTypeText(randomString)
    verify(colorsDetailView).disableColorOperations()
    verify(colorsDetailView).showMainImageWaitLoader()
    verify(colorsDetailView).showImageLoadError()
    verify(colorsDetailView).enableColorOperations()
  }
}