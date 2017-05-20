package dk.bison.wt

import android.util.Log
import dk.bison.wt.api.ApiProxy
import dk.bison.wt.api.Post
import dk.bison.wt.mvp.MvpBasePresenter
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Created by bison on 20-05-2017.
 */
class MainPresenter(val api: ApiProxy) : MvpBasePresenter<MainMvpView>() {
    init {
        val name : String = "Per"
        Log.i("", "Customer initialized with value ${name}")
    }

    override fun attachView(view: MainMvpView) {
        super.attachView(view)
        Log.e("debug", "attachView")
        loadPosts({ posts ->
            if (isViewAttached)
                view?.showPosts(posts)
        }, { errorCode, msg ->
            if (isViewAttached) {
                // showerror
            }
        })
    }

    override fun detachView() {
        super.detachView()
        Log.e("debug", "detachView")
    }

    fun loadPosts(onSuccess : (posts : List<Post>) -> Unit, onFailure : (errorCode : Int, msg : String) -> Unit)
    {
        api.getPosts().enqueue(object : Callback<List<Post>> {
            override fun onFailure(call: Call<List<Post>>?, t: Throwable?) {
                Log.e("debug", "onFailure")
                onFailure(-1, t?.message ?: "Unknown error")
            }

            override fun onResponse(call: Call<List<Post>>?, response: Response<List<Post>>?) {
                Log.e("debug", "got result")
                val posts : List<Post> = response?.body() ?: throw RuntimeException("Body could not be read")
                if(response.isSuccessful)
                {
                    onSuccess(posts)
                }
                else
                {
                    onFailure(500, response.message())
                }
            }

        })
    }
}