package dk.bison.wt

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import dk.bison.wt.api.Post
import dk.bison.wt.kstack.KStack
import dk.bison.wt.kstack.parseFromISO8601
import dk.bison.wt.kstack.translate.Translate
import kotlinx.coroutines.experimental.*
import java.util.*
import kotlin.system.measureTimeMillis
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), MainMvpView {
    private lateinit var presenter : MainPresenter

    //@Translate("defaultSection.cancel") var textTv : TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) = runBlocking<Unit> {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //textTv = findViewById(R.id.text) as TextView
        textTv.text = Translation.defaultSection.settings
        KStack.translate(this@MainActivity)

        presenter = MainPresenter(App.apiProxy())
        val d = Date()
        d.parseFromISO8601("2017-05-20T00:15:20+00:00")

        /*
        val job = launch(CommonPool) { // create new coroutine and keep a reference to its Job
            delay(1000L)
            println("World!")
            repeat(1000) { i ->
                println("I'm sleeping $i ...")
                delay(500L)
            }
        }
        println("Hello,")
        job.join() // wait until child coroutine completes
        */
        KStack.appOpen({
            //textTv.text = Translation.defaultSection.settings
        })
    }


    override fun onResume() {
        super.onResume()
        presenter.attachView(this)
        KStack.translate(this@MainActivity)
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
