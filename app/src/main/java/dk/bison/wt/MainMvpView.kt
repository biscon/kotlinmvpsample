package dk.bison.wt

import dk.bison.wt.api.Post
import dk.bison.wt.mvp.MvpView

/**
 * Created by bison on 20-05-2017.
 */
interface MainMvpView : MvpView {
    fun showPosts(posts : List<Post>)
}