package kr.co.kiocp.hcolor

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.SkuDetails
import kr.co.kiocp.hcolor.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private lateinit var bm: BillingModule

    private var mSkuDetails = listOf<SkuDetails>()
        set(value) {
            field = value
            setSkuDetailsView()
        }

    private var currentSubscription: Purchase? = null
        set(value) {
            field = value
            updateSubscriptionState()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bm = BillingModule(this, lifecycleScope, object : BillingModule.Callback {
            override fun onBillingModulesIsReady() {
                bm.querySkuDetail(
                    BillingClient.SkuType.SUBS,
                    Sku.SUB_YEAR
                ) { skuDetails ->
                    Log.d("결과", skuDetails.size.toString())
                    Log.d("결과", skuDetails.toString())
                    mSkuDetails = skuDetails
                }

                bm.checkSubscribed {
                    currentSubscription = it
                }
            }

            override fun onSuccess(purchase: Purchase) {
                currentSubscription = purchase
            }

            override fun onFailure(errorCode: Int) {
                when (errorCode) {
                    BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                        Toast.makeText(applicationContext, "이미 구입한 상품입니다.", Toast.LENGTH_LONG)
                            .show()
                    }
                    BillingClient.BillingResponseCode.USER_CANCELED -> {
                        Toast.makeText(applicationContext, "구매를 취소하셨습니다.", Toast.LENGTH_LONG).show()
                    }
                    else -> {
                        Toast.makeText(applicationContext, "error: $errorCode", Toast.LENGTH_LONG)
                            .show()
                    }
                }
            }

        })

        setClickListeners()
    }

    private fun setClickListeners() {
        with(binding) {
            // 광고 제거 구매 버튼 클릭
            btnYear.setOnClickListener {
                mSkuDetails.find { it.sku == Sku.SUB_YEAR }?.let { skuDetail ->
                    bm.purchase(skuDetail, currentSubscription)
                } ?: also {
                    Toast.makeText(applicationContext, "상품을 찾을 수 없습니다.", Toast.LENGTH_LONG).show()
                }
            }

            btnGetYearList.setOnClickListener {
                mSkuDetails.find { it.sku == Sku.SUB_YEAR }?.let { skuDetail ->
                    Log.d("결과", skuDetail.toString())
                } ?: also {
                    Toast.makeText(applicationContext, "상품을 찾을 수 없습니다.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun setSkuDetailsView() {
        val builder = StringBuilder()
        for (skuDetail in mSkuDetails) {
            builder.append("<${skuDetail.title}>\n")
            builder.append(skuDetail.price)
            builder.append("\n======================\n\n")
        }
        binding.tvSku.text = builder
    }

    private fun updateSubscriptionState() {
        currentSubscription?.let {
            Log.d("결과 - ordierId ",it.orderId )
            Log.d("결과 - obfuscatedAccountId ",it.accountIdentifiers?.obfuscatedAccountId.toString() )
            Log.d("결과 - obfuscatedProfileId ",it.accountIdentifiers?.obfuscatedProfileId.toString() )
            binding.tvSubscription.text = "구독중: ${it.sku} | 자동갱신: ${it.isAutoRenewing}"
        } ?: also {
            binding.tvSubscription.text = "구독안함"
        }
    }
}