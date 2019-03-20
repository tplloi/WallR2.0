package zebrostudio.wallr100.android.ui.minimal

import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import com.afollestad.dragselectrecyclerview.DragSelectTouchListener
import com.afollestad.dragselectrecyclerview.Mode.RANGE
import com.afollestad.materialcab.MaterialCab
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.activity_main.minimalBottomLayout
import kotlinx.android.synthetic.main.activity_main.minimalBottomLayoutFab
import kotlinx.android.synthetic.main.activity_main.spinner
import kotlinx.android.synthetic.main.fragment_minimal.minimalFragmentRecyclerView
import zebrostudio.wallr100.R
import zebrostudio.wallr100.android.ui.BaseFragment
import zebrostudio.wallr100.android.ui.adapters.MinimalImageAdapter
import zebrostudio.wallr100.android.utils.RecyclerViewItemDecorator
import zebrostudio.wallr100.android.utils.errorToast
import zebrostudio.wallr100.android.utils.gone
import zebrostudio.wallr100.android.utils.inflate
import zebrostudio.wallr100.android.utils.integerRes
import zebrostudio.wallr100.android.utils.invisible
import zebrostudio.wallr100.android.utils.stringRes
import zebrostudio.wallr100.android.utils.visible
import zebrostudio.wallr100.android.utils.withDelayOnMain
import zebrostudio.wallr100.presentation.adapters.MinimalRecyclerItemContract.MinimalRecyclerViewPresenter
import zebrostudio.wallr100.presentation.minimal.MinimalContract.MinimalPresenter
import zebrostudio.wallr100.presentation.minimal.MinimalContract.MinimalView
import javax.inject.Inject

class MinimalFragment : BaseFragment(), MinimalView {

  @Inject
  internal lateinit var presenter: MinimalPresenter
  @Inject
  internal lateinit var recyclerAdapterPresenter: MinimalRecyclerViewPresenter
  private var minimalImageAdapter: MinimalImageAdapter? = null
  private var touchListener: DragSelectTouchListener? = null

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    AndroidSupportInjection.inject(this)
    return container?.inflate(inflater, R.layout.fragment_minimal)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    initRecyclerView()
    initBottomPanel()
    presenter.attachView(this)
    presenter.attachMinimalImageRecyclerViewPresenter(recyclerAdapterPresenter)
    recyclerAdapterPresenter.attachMinimalPresenter(presenter)
    presenter.handleViewCreated()
  }

  override fun onDestroy() {
    presenter.detachView()
    presenter.detachMinimalImageRecyclerViewPresenter()
    recyclerAdapterPresenter.detachMinimalPresenter()
    super.onDestroy()
  }

  override fun updateAllItems() {
    minimalImageAdapter?.notifyDataSetChanged()
  }

  override fun showUnableToGetColorsErrorMessage() {
    context!!.let {
      it.errorToast(it.stringRes(R.string.minimal_fragment_unable_to_get_colors_error_message))
    }
  }

  override fun showGenericErrorMessage() {
    context!!.let {
      it.errorToast(it.stringRes(R.string.generic_error_message))
    }
  }

  override fun updateItemView(index: Int) {
    minimalImageAdapter?.notifyItemChanged(index)
  }

  override fun removeItemView(index: Int) {
    minimalImageAdapter?.notifyItemRemoved(index)
  }

  override fun showCab(size: Int) {
    MaterialCab.attach(activity as AppCompatActivity, R.id.cabStub) {
      menuRes = R.menu.minimal
      closeDrawableRes = R.drawable.ic_close_white
      titleColor = Color.WHITE
      title = context!!.stringRes(R.string.minimal_fragment_cab_title, size)

      onSelection {
        if (it.itemId == R.id.delete) {
          presenter.handleDeleteMenuItemClick()
        }
        true
      }

      onDestroy {
        presenter.handleCabDestroyed()
        true
      }
    }

  }

  override fun hideCab() {
    MaterialCab.destroy()
  }

  override fun showBottomPanelWithAnimation() {
    AnimationUtils.loadAnimation(context, R.anim.slide_up).apply {
      fillAfter = true
      setAnimationListener(object : Animation.AnimationListener {
        override fun onAnimationRepeat(animation: Animation) {

        }

        override fun onAnimationEnd(animation: Animation) {
          activity?.spinner?.isEnabled = true
          activity?.minimalBottomLayout?.visible()
          activity?.minimalBottomLayout?.visible()
        }

        override fun onAnimationStart(animation: Animation) {
          activity?.minimalBottomLayout?.isClickable = true
        }
      })
    }.let {
      activity?.minimalBottomLayout?.startAnimation(it)
    }

    AnimationUtils.loadAnimation(context, R.anim.grow_circular_reveal).apply {
      fillAfter = true
      setAnimationListener(object : Animation.AnimationListener {
        override fun onAnimationRepeat(animation: Animation) {

        }

        override fun onAnimationEnd(animation: Animation) {
          activity?.minimalBottomLayoutFab?.visible()
          activity?.minimalBottomLayoutFab?.isClickable = true
        }

        override fun onAnimationStart(animation: Animation) {
          activity?.minimalBottomLayoutFab?.isClickable = true
        }
      })
    }.let {
      activity?.minimalBottomLayoutFab?.startAnimation(it)
    }
  }

  override fun hideBottomLayoutWithAnimation() {
    AnimationUtils.loadAnimation(context, R.anim.slide_down).apply {
      fillAfter = true
      setAnimationListener(object : Animation.AnimationListener {
        override fun onAnimationRepeat(animation: Animation) {

        }

        override fun onAnimationEnd(animation: Animation) {
          activity?.spinner?.isEnabled = false
          activity?.minimalBottomLayout?.gone()
        }

        override fun onAnimationStart(animation: Animation) {
          activity?.minimalBottomLayout?.isClickable = false
        }
      })
    }.let {
      activity?.minimalBottomLayout?.startAnimation(it)
    }

    AnimationUtils.loadAnimation(context, R.anim.shrink_reverse_circular_reveal).apply {
      fillAfter = true
      setAnimationListener(object : Animation.AnimationListener {
        override fun onAnimationRepeat(animation: Animation) {

        }

        override fun onAnimationEnd(animation: Animation) {
          activity?.minimalBottomLayoutFab?.gone()
          activity?.minimalBottomLayoutFab?.isClickable = false
        }

        override fun onAnimationStart(animation: Animation) {

        }
      })
    }.let {
      activity?.minimalBottomLayoutFab?.startAnimation(it)
    }
  }

  override fun startSelection(position: Int) {
    touchListener?.setIsActive(true, position)
  }

  override fun showDeselectBeforeDeletionMessage(numberOfItemsToBeDeselected: Int) {
    context!!.let { context ->
      if (numberOfItemsToBeDeselected == 1) {
        context.stringRes(R.string.minimal_fragment_deselect_1_color_message)
      } else {
        context.stringRes(R.string.minimal_fragment_deselect_x_colors_message,
            numberOfItemsToBeDeselected)
      }.let {
        context.errorToast(it)
      }
    }
  }

  override fun showDeleteColorsErrorMessage() {
    context!!.let {
      it.errorToast(it.stringRes(R.string.minimal_fragment_delete_colors_error_message))
    }
  }

  override fun clearCabIfActive() {
    if (MaterialCab.isActive){
      MaterialCab.destroy()
    }
  }

  private fun initRecyclerView() {
    GridLayoutManager(context,
        context!!.integerRes(R.integer.minimal_image_recycler_view_span_count)).let {
      minimalFragmentRecyclerView.layoutManager = it
    }
    minimalFragmentRecyclerView.addItemDecoration(
        RecyclerViewItemDecorator(context!!.integerRes(R.integer.recycler_view_grid_spacing_px),
            context!!.integerRes(R.integer.minimal_image_recycler_view_grid_size)))
    minimalImageAdapter = MinimalImageAdapter(recyclerAdapterPresenter)
    minimalFragmentRecyclerView.adapter = minimalImageAdapter
    touchListener = DragSelectTouchListener.create(context!!, minimalImageAdapter!!) {
      this.mode = RANGE
    }
    minimalFragmentRecyclerView.addOnItemTouchListener(touchListener!!)
    minimalFragmentRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
      override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)
        presenter.handleScroll(dy)
      }
    })
  }

  private fun initBottomPanel() {
    activity?.spinner?.setItems(
        context!!.stringRes(R.string.minimal_fragment_spinner_item_material),
        context!!.stringRes(R.string.minimal_fragment_spinner_item_gradient),
        context!!.stringRes(R.string.minimal_fragment_spinner_item_plasma))
    activity?.spinner?.setOnItemSelectedListener { _, position, _, _ ->
      presenter.handleSpinnerOptionChanged(position)
    }
    showBottomPanelAndFab()
    withDelayOnMain(600) { hideBottomLayoutWithAnimation() }
  }

  private fun showBottomPanelAndFab(){
    activity?.minimalBottomLayout?.invisible()
    activity?.minimalBottomLayout?.isClickable = false
    activity?.minimalBottomLayoutFab?.invisible()
    activity?.minimalBottomLayoutFab?.isClickable = false
  }

  companion object {
    fun newInstance() = MinimalFragment()
  }

}