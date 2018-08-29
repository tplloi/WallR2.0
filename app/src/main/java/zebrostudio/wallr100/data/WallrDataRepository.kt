package zebrostudio.wallr100.data

import io.reactivex.Single
import zebrostudio.wallr100.data.api.RemoteAuthServiceFactory
import zebrostudio.wallr100.data.api.UrlMap
import zebrostudio.wallr100.data.exception.InvalidPurchaseException
import zebrostudio.wallr100.data.exception.UnableToVerifyPurchaseException
import zebrostudio.wallr100.domain.WallrRepository

class WallrDataRepository(
  private var retrofitFirebaseAuthFactory: RemoteAuthServiceFactory,
  private var sharedPrefsHelper: SharedPrefsHelper
) : WallrRepository {

  private val premiumUserTag = "premium_user"

  override fun authenticatePurchase(
    packageName: String,
    skuId: String,
    purchaseToken: String
  ): Single<Any> {

    return retrofitFirebaseAuthFactory.verifyPurchaseService()
        .verifyPurchase(UrlMap.getFirebasePurchaseAuthEndpoint(packageName, skuId, purchaseToken))
        .flatMap {
          if (it.status == "success") {
            Single.just(it)
          } else if (it.status == "error" && (it.errorCode == 404 || it.errorCode == 403)) {
            Single.error(InvalidPurchaseException())
          } else {
            Single.error(UnableToVerifyPurchaseException())
          }
        }
  }

  override fun updateUserPurchaseStatus(): Boolean {
    return sharedPrefsHelper.setBoolean(premiumUserTag, true)
  }

  override fun isUserPremium(): Boolean {
    return sharedPrefsHelper.getBoolean(premiumUserTag, false)
  }

}