package zebrostudio.wallr100.presentation.minimal

import com.uber.autodispose.autoDisposable
import zebrostudio.wallr100.android.ui.adapters.MinimalViewHolder
import zebrostudio.wallr100.data.exception.UnableToGetSolidColorsException
import zebrostudio.wallr100.domain.executor.PostExecutionThread
import zebrostudio.wallr100.domain.interactor.MinimalImagesUseCase
import zebrostudio.wallr100.presentation.minimal.MinimalContract.ItemViewHolder
import zebrostudio.wallr100.presentation.minimal.MinimalContract.MinimalPresenter
import zebrostudio.wallr100.presentation.minimal.MinimalContract.MinimalView
import zebrostudio.wallr100.presentation.minimal.MultiColorImageType.GRADIENT
import zebrostudio.wallr100.presentation.minimal.MultiColorImageType.MATERIAL
import zebrostudio.wallr100.presentation.minimal.MultiColorImageType.PLASMA
import java.util.Collections
import java.util.TreeMap

const val MINIMUM_SCROLL_DIST = 15
const val INITIAL_SIZE = 0
const val INITIAL_OFFSET = 1
const val MINIMUM_COLOR_LIST_SIZE = 20
const val FIRST_ELEMENT_INDEX = 1

class MinimalPresenterImpl(
  private val minimalImagesUseCase: MinimalImagesUseCase,
  private val postExecutionThread: PostExecutionThread
) : MinimalPresenter {

  private var isBottomPanelEnabled = false
  private var selectionSize = INITIAL_SIZE
  private var minimalView: MinimalView? = null
  private var forceSmoothScroll: Boolean = false
  private var multiColorImageType: MultiColorImageType? = MATERIAL
  private var colorList = mutableListOf<String>()
  private var selectedHashMap = HashMap<Int, Boolean>()

  override fun attachView(view: MinimalView) {
    minimalView = view
  }

  override fun detachView() {
    minimalView = null
  }

  override fun handleViewCreated() {
    if (minimalImagesUseCase.isCustomColorListPresent()) {
      minimalImagesUseCase.getCustomColors()
    } else {
      minimalImagesUseCase.getDefaultColors()
    }.observeOn(postExecutionThread.scheduler)
        .autoDisposable(minimalView!!.getScope())
        .subscribe({
          colorList = it.toMutableList()
          minimalView?.updateAllItems()
        }, {
          if (it is UnableToGetSolidColorsException) {
            minimalView?.showUnableToGetColorsErrorMessage()
          } else {
            minimalView?.showGenericErrorMessage()
          }
        })
  }

  override fun updateSelectionChange(index: Int, size: Int) {
    selectionSize = size
    minimalView?.updateItemView(index)
    if (selectionSize == FIRST_ELEMENT_INDEX) {
      minimalView?.showCab(selectionSize)
      if (isBottomPanelEnabled) {
        minimalView?.hideBottomLayoutWithAnimation()
        isBottomPanelEnabled = false
      }
    } else if (selectionSize > FIRST_ELEMENT_INDEX && !isBottomPanelEnabled) {
      isBottomPanelEnabled = true
      minimalView?.showBottomPanelWithAnimation()
      minimalView?.showCab(selectionSize)
    } else if (selectionSize > FIRST_ELEMENT_INDEX && isBottomPanelEnabled) {
      minimalView?.showCab(selectionSize)
    } else if (selectionSize == INITIAL_SIZE) {
      isBottomPanelEnabled = false
      minimalView?.hideBottomLayoutWithAnimation()
      minimalView?.hideCab()
    }
  }

  override fun handleItemLongClick(position: Int) {
    minimalView?.startSelection(position)
  }

  override fun handleScroll(yAxisMovement: Int) {
    if (isBottomPanelEnabled && yAxisMovement > MINIMUM_SCROLL_DIST && !forceSmoothScroll) {
      minimalView?.hideBottomLayoutWithAnimation()
      isBottomPanelEnabled = false
    } else if (!isBottomPanelEnabled && yAxisMovement < -MINIMUM_SCROLL_DIST && selectionSize > 1) {
      minimalView?.showBottomPanelWithAnimation()
      isBottomPanelEnabled = true
    }
  }

  override fun handleDeleteMenuItemClick() {
    numberOfItemsToBeDeselectedToStartDeletion().let {
      if (it == INITIAL_SIZE) {
        val reversedSelectedItems = TreeMap<Int, Boolean>(Collections.reverseOrder())
        minimalImagesUseCase.modifyColors(colorList, selectedHashMap)
            .doOnSuccess {
              reversedSelectedItems.putAll(selectedHashMap)
              clearSelectedItems()
              colorList = it.toMutableList()
            }
            .observeOn(postExecutionThread.scheduler)
            .autoDisposable(minimalView!!.getScope())
            .subscribe({
              reversedSelectedItems.keys.forEach {
                minimalView?.removeItemView(it)
              }
              minimalView?.clearCabIfActive()
            }, {
              System.out.println(it.printStackTrace())
              minimalView?.clearCabIfActive()
              minimalView?.showDeleteColorsErrorMessage()
            })
      } else {
        minimalView?.showDeselectBeforeDeletionMessage(it)
      }
    }
  }

  override fun handleCabDestroyed() {
    clearSelectedItems()
    if (isBottomPanelEnabled) {
      minimalView?.hideBottomLayoutWithAnimation()
      isBottomPanelEnabled = false
      selectionSize = INITIAL_SIZE
    }
    minimalView?.updateAllItems()
  }

  override fun handleSpinnerOptionChanged(position: Int) {
    when (position) {
      MATERIAL.ordinal -> multiColorImageType = MATERIAL
      GRADIENT.ordinal -> multiColorImageType = GRADIENT
      PLASMA.ordinal -> multiColorImageType = PLASMA
    }
  }

  override fun handleColorPickerPositiveClick(text: String) {

  }

  override fun getItemCount(): Int {
    return colorList.size
  }

  override fun onBindRepositoryRowViewAtPosition(holder: MinimalViewHolder, position: Int) {
    if (position == INITIAL_SIZE) {
      holder.showAddImageLayout()
      holder.hideSelectedIndicator()
    } else {
      holder.hideAddImageLayout()
      holder.setImageViewColor(colorList[position - INITIAL_OFFSET])
      holder.attachLongClickListener()
      if (selectedHashMap.size != 0) {
        if (selectedHashMap.containsKey(position - INITIAL_OFFSET)) {
          holder.showSelectedIndicator()
        } else {
          holder.hideSelectedIndicator()
        }
      }
    }
    holder.attachClickListener()
  }

  override fun handleClick(
    position: Int,
    itemView: ItemViewHolder
  ) {
    if (selectedHashMap.size == 0) {

    } else {
      toggleSelected(position)
    }
  }

  override fun handleImageLongClick(
    position: Int,
    itemView: ItemViewHolder
  ) {
    toggleSelected(position)
    handleItemLongClick(position)

  }

  override fun isItemSelectable(index: Int): Boolean {
    return index != INITIAL_SIZE
  }

  override fun isItemSelected(index: Int): Boolean {
    return selectedHashMap.containsKey(index - INITIAL_OFFSET)
  }

  override fun setItemSelected(index: Int, selected: Boolean) {
    if (selected) {
      selectedHashMap[index - INITIAL_OFFSET] = true
    } else {
      selectedHashMap.remove(index - INITIAL_OFFSET)
    }
    updateSelectionChange(index, selectedHashMap.size)
  }

  override fun clearSelectedItems() {
    selectedHashMap.clear()
  }

  override fun numberOfItemsToBeDeselectedToStartDeletion(): Int {
    return if (colorList.size - selectedHashMap.size >= MINIMUM_COLOR_LIST_SIZE) {
      INITIAL_SIZE
    } else {
      MINIMUM_COLOR_LIST_SIZE - (colorList.size - selectedHashMap.size)
    }
  }

  private fun toggleSelected(index: Int) {
    (index - INITIAL_OFFSET).let {
      if (!selectedHashMap.containsKey(it)) {
        selectedHashMap.put(it, true)
      } else {
        selectedHashMap.remove(it)
      }
    }
    updateSelectionChange(index, selectedHashMap.size)
  }

}

enum class MultiColorImageType {
  MATERIAL,
  GRADIENT,
  PLASMA
}