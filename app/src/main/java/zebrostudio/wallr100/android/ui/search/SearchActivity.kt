package zebrostudio.wallr100.android.ui.search

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.speech.RecognizerIntent
import android.support.design.widget.AppBarLayout
import android.text.TextUtils
import android.view.View
import com.miguelcatalan.materialsearchview.MaterialSearchView
import dagger.android.AndroidInjection
import kotlinx.android.synthetic.main.activity_search.SearchActivitySpinkitView
import kotlinx.android.synthetic.main.activity_search.noInputRelativeLayout
import kotlinx.android.synthetic.main.activity_search.noResultRelativeLayout
import kotlinx.android.synthetic.main.activity_search.searchAppBar
import kotlinx.android.synthetic.main.activity_search.searchView
import zebrostudio.wallr100.R
import zebrostudio.wallr100.android.utils.withDelayOnMain
import zebrostudio.wallr100.presentation.search.SearchContract
import javax.inject.Inject

class SearchActivity : AppCompatActivity(), SearchContract.SearchView {

  @Inject
  internal lateinit var presenter: SearchContract.SearchPresenter

  private var appBarCollapsed = false

  override fun onCreate(savedInstanceState: Bundle?) {
    AndroidInjection.inject(this)
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_search)
    presenter.attachView(this)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      overridePendingTransition(R.anim.slide_in_up, 0)
    }
    initAppbar()
    initSearchView()
    hideLoader()
    hideNoResultView()
  }

  override fun onDestroy() {
    presenter.detachView()
    super.onDestroy()
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
    if (requestCode == MaterialSearchView.REQUEST_VOICE && resultCode == Activity.RESULT_OK) {
      val matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
      if (matches != null && matches.size > 0) {
        val searchWrd = matches[0]
        if (!TextUtils.isEmpty(searchWrd)) {
          searchView.setQuery(searchWrd, false)
        }
      }
      return
    }
  }

  override fun onBackPressed() {
    when {
      appBarCollapsed -> {
        searchAppBar.setExpanded(true, true)
        withDelayOnMain(300, block = {
          if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            finish()
            overridePendingTransition(0, R.anim.slide_out_down)
          } else {
            appBarCollapsed = false
            val params = searchView?.layoutParams as AppBarLayout.LayoutParams
            params.scrollFlags = 0
            onBackPressed()
          }
        })
      }
      Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP -> {
        overridePendingTransition(0, R.anim.slide_out_down)
        finish()
      }
      else -> super.onBackPressed()
    }
  }

  override fun showLoader() {
    SearchActivitySpinkitView.visibility = View.VISIBLE
  }

  override fun hideLoader() {
    SearchActivitySpinkitView.visibility = View.GONE
  }

  override fun showNoInputView() {
    noInputRelativeLayout.visibility = View.VISIBLE
  }

  override fun hideNoInputView() {
    noInputRelativeLayout.visibility = View.GONE
  }

  override fun showNoResultView() {
    noResultRelativeLayout.visibility = View.VISIBLE
  }

  override fun hideNoResultView() {
    noResultRelativeLayout.visibility = View.VISIBLE
  }

  private fun initAppbar() {
    searchAppBar.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
      when {
        Math.abs(verticalOffset) == appBarLayout.totalScrollRange -> {
          // Collapsed
          appBarCollapsed = true
        }
        verticalOffset == 0 -> {
          // Expanded
          appBarCollapsed = false
        }
      }
    }
  }

  private fun initSearchView() {
    searchView.backButton.setOnClickListener { onBackPressed() }
    searchView.setVoiceSearch(true)
    searchView.showSearch()
    setSearchListener()
  }

  private fun setSearchListener() {
    searchView.setOnQueryTextListener(object : MaterialSearchView.OnQueryTextListener {
      override fun onQueryTextSubmit(query: String?): Boolean {
        noInputRelativeLayout.visibility = View.GONE
        searchView.hideKeyboard(currentFocus)
        presenter.notifyQuerySubmitted(query)
        return true
      }

      override fun onQueryTextChange(newText: String?): Boolean {
        return false
      }

    })

  }
}
