package zebrostudio.wallr100.presentation.search

import zebrostudio.wallr100.data.exception.NoResultFoundException
import zebrostudio.wallr100.domain.interactor.SearchPicturesUseCase
import zebrostudio.wallr100.presentation.search.mapper.SearchPicturesPresenterEntityMapper

class SearchPresenterImpl(
  private var retrievePicturesUseCase: SearchPicturesUseCase,
  private var searchPicturesPresenterEntityMapper: SearchPicturesPresenterEntityMapper
) :
    SearchContract.SearchPresenter {

  private var searchView: SearchContract.SearchView? = null
  private var queryPage = 1
  private var keyword: String? = null

  override fun attachView(view: SearchContract.SearchView) {
    searchView = view
  }

  override fun detachView() {
    searchView = null
  }

  override fun notifyQuerySubmitted(query: String?) {
    queryPage = 1
    searchView?.hideAll()
    searchView?.showLoader()
    keyword = query
    retrievePicturesUseCase.buildRetrievePicturesSingle(getQueryString(keyword))
        .map {
          searchPicturesPresenterEntityMapper.mapToPresenterEntity(it)
        }
        .subscribe({
          searchView?.hideLoader()
          searchView?.showSearchResults(it)
          queryPage++
        }, {
          when (it) {
            is NoResultFoundException -> searchView?.showNoResultView(keyword)
            else -> {
              if (it.message != null && it.message == "Unable to resolve host \"api.unsplash.com\"" +
                  ": No address associated with hostname") {
                searchView?.showNoInternetView()
              } else {
                searchView?.showGenericErrorView()
              }
            }
          }
        })
  }

  override fun fetchMoreImages() {
    searchView?.showBottomLoader()
    retrievePicturesUseCase.buildRetrievePicturesSingle(getQueryString(keyword))
        .map {
          searchPicturesPresenterEntityMapper.mapToPresenterEntity(it)
        }
        .subscribe({
          searchView?.hideBottomLoader()
          searchView?.appendSearchResults(((queryPage - 1) * 30), it) // 30 results per page
          queryPage++
        }, {
          when (it) {
            is NoResultFoundException -> searchView?.showNoResultView(keyword)
            else -> {
              if (it.message != null && it.message == "Unable to resolve host \"api.unsplash.com\"" +
                  ": No address associated with hostname") {
                searchView?.showNoInternetToast()
              } else {
                searchView?.showGenericErrorToast()
              }
            }
          }
        })
  }

  private fun getQueryString(keyword: String?): String {
    return "photos/search?query=$keyword&per_page=30&page=$queryPage"
  }

}