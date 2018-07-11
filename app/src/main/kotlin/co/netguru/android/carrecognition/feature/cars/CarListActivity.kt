package co.netguru.android.carrecognition.feature.cars

import android.animation.Animator
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewAnimationUtils
import co.netguru.android.carrecognition.R
import co.netguru.android.carrecognition.common.extensions.onGlobalLayout
import co.netguru.android.carrecognition.common.extensions.onPageSelected
import co.netguru.android.carrecognition.data.db.Cars
import com.hannesdorfmann.mosby3.mvp.MvpActivity
import dagger.android.AndroidInjection
import kotlinx.android.synthetic.main.car_list_view.*
import kotlinx.android.synthetic.main.circle_progress_bar_with_label.*
import javax.inject.Inject


class CarListActivity : MvpActivity<CarListContract.View, CarListContract.Presenter>(),
        CarListContract.View {

    @Inject
    lateinit var carListPresenter: CarListContract.Presenter
    private var currentVisibleItem = -1
    private lateinit var carAdapter: CarsPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.car_list_view)
        setPresenter(createPresenter())

        if (savedInstanceState == null) {
            root_layout.visibility = View.INVISIBLE
            onGlobalLayout {
                initViewPager()
                showCircularAnimation(false)
            }
        }

        back_arrow.setOnClickListener { showCircularAnimation(true) }
        camera_button.setOnClickListener { showCircularAnimation(true) }

        presenter.apply {
            attachView(this@CarListActivity)
            getCars()
        }
    }

    private fun initViewPager() {
        view_pager.apply {
            offscreenPageLimit = 3
            pageMargin = -rootView.width / 10 //set side pages to be visible in 10%
            carAdapter = CarsPagerAdapter(getCarIdOpt())
            adapter = carAdapter
            presenter.onAdapterReady()
            onPageSelected { position ->
                if (position == currentVisibleItem) return@onPageSelected
                currentVisibleItem = position
                carAdapter.showAnimation(position)
            }
            setPageTransformer(false, CarListPageTransformer())
        }
    }

    private fun showCircularAnimation(hide: Boolean) {
        val maxRadius = Math.max(root_layout.width, root_layout.height).toFloat()
        val finalRadius = if (hide) 0f else maxRadius
        val startRadius = if (hide) maxRadius else 0f
        ViewAnimationUtils.createCircularReveal(root_layout,
                getStartXOpt(0), getStartYOpt(root_layout.height),
                startRadius, finalRadius).apply {
            if (hide) finishOnAnimationEnd()
            else root_layout.visibility = View.VISIBLE
            duration = REVEAL_ANIM_DURATION
            start()
        }
    }

    private fun Animator.finishOnAnimationEnd() {
        addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator?) {}
            override fun onAnimationCancel(animation: Animator?) {}
            override fun onAnimationRepeat(animation: Animator?) {}
            override fun onAnimationEnd(animation: Animator?) {
                root_layout.visibility = View.INVISIBLE
                finish()
            }
        })
    }

    override fun showLoading(show: Boolean) {
        loading_bar.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun populate(cars: List<Cars>) {
        showLoading(false)
        setSeenCarsProgress(cars.count { it.seen }, cars.size)
        carAdapter.populate(cars)
        view_pager.currentItem = cars.find { it.id == getCarIdOpt() }?.let {
            cars.indexOf(it)
        } ?: 0
    }

    override fun onError(throwable: Throwable) {
        showLoading(false)
    }

    private fun setSeenCarsProgress(seen: Int, carsSize: Int) {
        progressBar.apply {
            max = carsSize
            progress = seen
        }
        progressText.apply {
            text = ("$seen/$carsSize")
        }
    }

    override fun onBackPressed() {
        showCircularAnimation(true)
    }

    override fun onDestroy() {
        presenter.onActivityDestroy()
        super.onDestroy()
    }

    override fun createPresenter(): CarListContract.Presenter = carListPresenter

    companion object {
        private const val REVEAL_ANIM_DURATION = 400L
        private const val START_X = "startX"
        private const val START_Y = "startY"
        private const val CAR_ID = "carId"
        fun startActivityWithCircleAnimation(activity: Activity, startX: Int, startY: Int,
                                             carId: String? = null) {
            activity.startActivity(
                    Intent(activity, CarListActivity::class.java).apply {
                        putExtra(START_X, startX)
                        putExtra(START_Y, startY)
                        carId?.also { putExtra(CAR_ID, it) }
                    })
        }

        private fun Activity.getStartXOpt(default: Int) = intent.getIntExtra(START_X, default)
        private fun Activity.getStartYOpt(default: Int) = intent.getIntExtra(START_Y, default)
        private fun Activity.getCarIdOpt() = intent.getStringExtra(CAR_ID)
    }
}
