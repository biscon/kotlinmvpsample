package dk.bison.wt

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import dk.bison.wt.api.Post
import dk.bison.wt.kstack.KStack
import dk.bison.wt.kstack.translate.Translate
import kotlinx.coroutines.experimental.*
import java.util.*
import kotlin.system.measureTimeMillis

class MainActivity : AppCompatActivity(), MainMvpView {
    private lateinit var presenter : MainPresenter

    @Translate("defaultSection.cancel") var textTv : TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) = runBlocking<Unit> {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        textTv = findViewById(R.id.text) as TextView
        //textTv.text = Translation.defaultSection.settings
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
        work()

    }


    suspend fun doSomethingUsefulOne(): Int {
        delay(1000L) // pretend we are doing something useful here
        return 13
    }

    suspend fun doSomethingUsefulTwo(): Int {
        delay(1000L) // pretend we are doing something useful here, too
        return 29
    }

    fun work() = runBlocking<Unit> {
        val time = measureTimeMillis {
            val one = async(CommonPool) { doSomethingUsefulOne() }
            val two = async(CommonPool) { doSomethingUsefulTwo() }
            println("The answer is ${one.await() + two.await()}")
        }
        println("Completed in $time ms")
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
