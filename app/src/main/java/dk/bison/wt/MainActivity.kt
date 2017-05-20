package dk.bison.wt

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import dk.bison.wt.api.Post
import java.util.*

class MainActivity : AppCompatActivity(), MainMvpView {
    private lateinit var presenter : MainPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        presenter = MainPresenter(App.apiProxy())
        val d = Date()
        d.parseFromISO8601("2017-05-20T00:15:20+00:00")
        
    }

    override fun onResume() {
        super.onResume()
        presenter.attachView(this)
    }

    override fun onPause() {
        super.onPause()
        presenter.detachView()
    }

    override fun showPosts(posts: List<Post>) {
        for(post in posts)
        {
            Log.d("debug", post.toString())
        }
    }
}
