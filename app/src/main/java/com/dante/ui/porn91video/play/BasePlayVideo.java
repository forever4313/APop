package com.dante.ui.porn91video.play;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.dante.R;
import com.dante.adapter.PlayFragmentAdapter;
import com.dante.custom.TastyToast;
import com.dante.data.model.UnLimit91PornItem;
import com.dante.data.model.VideoResult;
import com.dante.service.DownloadVideoService;
import com.dante.ui.MvpActivity;
import com.dante.ui.porn91video.author.AuthorFragment;
import com.dante.ui.porn91video.comment.CommentFragment;
import com.dante.ui.user.UserLoginActivity;
import com.dante.utils.DialogUtils;
import com.dante.utils.LoadHelperUtils;
import com.dante.utils.constants.Keys;
import com.dante.utils.constants.KeysActivityRequestResultCode;
import com.github.rubensousa.floatingtoolbar.FloatingToolbar;
import com.helper.loadviewhelper.help.OnLoadViewListener;
import com.helper.loadviewhelper.load.LoadViewHelper;
import com.jaeger.library.StatusBarUtil;
import com.orhanobut.logger.Logger;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * @author flymegoc
 */
public abstract class BasePlayVideo extends MvpActivity<PlayVideoView, PlayVideoPresenter> implements PlayVideoView {

    private final String TAG = BasePlayVideo.class.getSimpleName();

    @BindView(R.id.floatingToolbar)
    FloatingToolbar floatingToolbar;
    @BindView(R.id.fab)
    FloatingActionButton fab;
    @BindView(R.id.app_bar_layout)
    AppBarLayout appBarLayout;
    @BindView(R.id.tv_play_video_title)
    TextView tvPlayVideoTitle;
    @BindView(R.id.tv_play_video_author)
    TextView tvPlayVideoAuthor;
    @BindView(R.id.tv_play_video_add_date)
    TextView tvPlayVideoAddDate;
    @BindView(R.id.tv_play_video_info)
    TextView tvPlayVideoInfo;
    @BindView(R.id.coordinator)
    CoordinatorLayout coordinator;

    @BindView(R.id.video_player_container)
    FrameLayout videoPlayerContainer;
    @BindView(R.id.tab_play)
    TabLayout tabPlay;
    @BindView(R.id.viewPager_play)
    ViewPager viewPagerPlay;

    private AlertDialog mAlertDialog;
    private AlertDialog favoriteDialog;

    private LoadViewHelper helper;

    protected UnLimit91PornItem unLimit91PornItem;
    @Inject
    protected CommentFragment commentFragment;

    @Inject
    protected AuthorFragment authorFragment;

    @Inject
    protected PlayFragmentAdapter playFragmentAdapter;

    @Inject
    protected PlayVideoPresenter playVideoPresenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base_play_video);
        ButterKnife.bind(this);
        initPlayerView();
        initIntentData();
        initDialog();
        initLoadHelper();
        initData();
        initBottomMenu();

        initTab();
    }

    private void initIntentData() {
        unLimit91PornItem = (UnLimit91PornItem) getIntent().getSerializableExtra(Keys.KEY_INTENT_UNLIMIT91PORNITEM);
    }

    /**
     * 底部切换标签tab
     */
    private void initTab() {
        List<Fragment> fragments = new ArrayList<>();
        fragments.add(commentFragment);
        fragments.add(authorFragment);
        playFragmentAdapter.setData(fragments);
        viewPagerPlay.setAdapter(playFragmentAdapter);
        tabPlay.setupWithViewPager(viewPagerPlay);
    }

    /**
     * 初始化视频引擎视图
     */
    public abstract void initPlayerView();

    public void initData() {
        UnLimit91PornItem tmp = dataManager.findUnLimit91PornItemByViewKey(unLimit91PornItem.getViewKey());
        //登录之后，第一次需要刷新获取uid,否则无法使用收藏功能
        if (tmp == null || tmp.getVideoResultId() == 0 || presenter.isLoadForUid()) {
            if (tmp == null) {
                presenter.loadVideoUrl(unLimit91PornItem);
            } else {
                presenter.loadVideoUrl(tmp);
            }
        } else {
            unLimit91PornItem = tmp;
            videoPlayerContainer.setVisibility(View.VISIBLE);
            Logger.t(TAG).d("使用已有播放地址");
            //浏览历史
            unLimit91PornItem.setViewHistoryDate(new Date());
            presenter.updateUnLimit91PornItem(unLimit91PornItem);
            VideoResult videoResult = unLimit91PornItem.getVideoResult();
            setToolBarLayoutInfo(unLimit91PornItem);

            playVideo(unLimit91PornItem.getTitle(), videoResult.getVideoUrl(), videoResult.getVideoName(), videoResult.getThumbImgUrl());
            //加载评论
            if (commentFragment != null) {
                commentFragment.setV9PornItem(unLimit91PornItem);
                commentFragment.loadVideoComment(videoResult.getVideoId(), unLimit91PornItem.getViewKey(), true);
            }
            if (authorFragment != null) {
                authorFragment.setV9PornItem(unLimit91PornItem);
            }
        }
    }


    private void setToolBarLayoutInfo(final UnLimit91PornItem v9PornItem) {
        if (v9PornItem.getVideoResultId() == 0) {
            return;
        }
        String searchTitleTag = "...";
        VideoResult videoResult = v9PornItem.getVideoResult();
        if (v9PornItem.getTitle().contains(searchTitleTag) || v9PornItem.getTitle().endsWith(searchTitleTag)) {
            tvPlayVideoTitle.setText(videoResult.getVideoName());
        } else {
            tvPlayVideoTitle.setText(v9PornItem.getTitle());
        }
        tvPlayVideoAuthor.setText(videoResult.getOwnnerName());
        tvPlayVideoAddDate.setText(videoResult.getAddDate());
        tvPlayVideoInfo.setText(videoResult.getUserOtherInfo());
    }

    private void initLoadHelper() {
        helper = new LoadViewHelper(viewPagerPlay);
        helper.setListener(new OnLoadViewListener() {
            @Override
            public void onRetryClick() {
                presenter.loadVideoUrl(unLimit91PornItem);
            }
        });
    }

    private void initDialog() {
        mAlertDialog = DialogUtils.initLodingDialog(this, "视频地址解析中...");
        favoriteDialog = DialogUtils.initLodingDialog(this, "收藏中,请稍后...");
    }

    private void initBottomMenu() {
        floatingToolbar.attachFab(fab);
        floatingToolbar.setClickListener(new FloatingToolbar.ItemClickListener() {
            @Override
            public void onItemClick(MenuItem item) {
                onOptionsItemSelected(item);
            }

            @Override
            public void onItemLongClick(MenuItem item) {

            }
        });
    }

    /**
     * 开始播放视频
     *
     * @param title      视频标题
     * @param videoUrl   视频链接
     * @param name       视频名字
     * @param thumImgUrl 视频缩略图
     */
    public abstract void playVideo(String title, String videoUrl, String name, String thumImgUrl);


    @NonNull
    @Override
    public PlayVideoPresenter createPresenter() {
        getActivityComponent().inject(this);

        return playVideoPresenter;
    }

    @Override
    public void showParsingDialog() {
        if (mAlertDialog == null) {
            return;
        }
        mAlertDialog.show();
    }

    @Override
    public void parseVideoUrlSuccess(UnLimit91PornItem v9PornItem) {
        this.unLimit91PornItem = v9PornItem;
        videoPlayerContainer.setVisibility(View.VISIBLE);
        setToolBarLayoutInfo(v9PornItem);
        VideoResult videoResult = v9PornItem.getVideoResult();
        //开始播放
        playVideo(unLimit91PornItem.getTitle(), videoResult.getVideoUrl(), videoResult.getVideoName(), videoResult.getThumbImgUrl());
        helper.showContent();
        if (commentFragment != null) {
            commentFragment.setV9PornItem(v9PornItem);
            commentFragment.loadVideoComment(videoResult.getVideoId(), v9PornItem.getViewKey(), true);
        }
        if (authorFragment != null) {
            authorFragment.setV9PornItem(v9PornItem);
        }
        dismissDialog();
    }

    @Override
    public void errorParseVideoUrl(String errorMessage) {
        dismissDialog();
        helper.showError();
        LoadHelperUtils.setErrorText(helper.getLoadError(), R.id.tv_error_text, "解析视频地址失败了，点击重试");
        showMessage(errorMessage, TastyToast.ERROR);
    }

    @Override
    public void favoriteSuccess() {
        presenter.setFavoriteNeedRefresh(true);
        showMessage("收藏成功", TastyToast.SUCCESS);
    }

    @Override
    public void showError(String message) {
        showMessage(message, TastyToast.ERROR);
        dismissDialog();
    }

    @Override
    public void showLoading(boolean pullToRefresh) {
        helper.showLoading();
        LoadHelperUtils.setLoadingText(helper.getLoadIng(), R.id.tv_loading_text, "拼命加载评论中...");
    }

    @Override
    public void showContent() {
        helper.showContent();
        dismissDialog();
    }

    @Override
    public void showMessage(String msg, int type) {
        super.showMessage(msg, type);
        dismissDialog();
    }

    private void dismissDialog() {
        if (mAlertDialog != null && mAlertDialog.isShowing() && !isFinishing()) {
            mAlertDialog.dismiss();
        }
        if (favoriteDialog != null && favoriteDialog.isShowing() && !isFinishing()) {
            favoriteDialog.dismiss();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.play_video, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.menu_play_collect) {
            favoriteVideo();
            return true;
        } else if (id == R.id.menu_play_download) {
            startDownloadVideo();
            return true;
        } else if (id == R.id.menu_play_share) {
            shareVideoUrl();
            return true;
        } else if (id == R.id.menu_play_comment) {
            showMessage("向下滑动即可评论", TastyToast.INFO);
            return true;
        } else if (id == R.id.menu_play_close) {
            floatingToolbar.hide();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void startDownloadVideo() {
        presenter.downloadVideo(unLimit91PornItem, true,false);
        Intent intent = new Intent(this, DownloadVideoService.class);
        startService(intent);
    }

    private void favoriteVideo() {
        if (unLimit91PornItem == null || unLimit91PornItem.getVideoResultId() == 0) {
            showMessage("还未成功解析视频链接，不能收藏！", TastyToast.INFO);
            return;
        }
        VideoResult videoResult = unLimit91PornItem.getVideoResult();
        if (!presenter.isUserLogin()) {
            goToLogin(KeysActivityRequestResultCode.LOGIN_ACTION_FOR_GET_UID);
            showMessage("请先登录", TastyToast.SUCCESS);
            return;
        }
        if (Integer.parseInt(videoResult.getOwnerId()) == presenter.getLoginUserId()) {
            showMessage("不能收藏自己的视频", TastyToast.WARNING);
            return;
        }
        favoriteDialog.show();
        presenter.favorite(String.valueOf(presenter.getLoginUserId()), videoResult.getVideoId(), videoResult.getOwnerId());
    }

    private void shareVideoUrl() {
        if (unLimit91PornItem == null || unLimit91PornItem.getVideoResultId() == 0) {
            showMessage("还未成功解析视频链接，不能分享！", TastyToast.INFO);
            return;
        }
        String url = unLimit91PornItem.getVideoResult().getVideoUrl();
        if (TextUtils.isEmpty(url)) {
            showMessage("还未成功解析视频链接，不能分享！", TastyToast.INFO);
            return;
        }
        Intent textIntent = new Intent(Intent.ACTION_SEND);
        textIntent.setType("text/plain");
        textIntent.putExtra(Intent.EXTRA_TEXT, "链接：" + url);
        startActivity(Intent.createChooser(textIntent, "分享视频地址"));
    }

    /**
     * 去登录
     *
     * @param actionKey 登录之后的动作key
     */
    private void goToLogin(int actionKey) {
        Intent intent = new Intent(this, UserLoginActivity.class);
        intent.putExtra(Keys.KEY_INTENT_LOGIN_FOR_ACTION, actionKey);
        startActivityWithAnimotion(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == KeysActivityRequestResultCode.RESULT_FOR_LOOK_AUTHOR_VIDEO) {
            if (authorFragment != null) {
                authorFragment.loadAuthorVideos();
            }
        } else if (resultCode == KeysActivityRequestResultCode.RESULT_CODE_FOR_REFRESH_GET_UID) {
            Logger.t(TAG).d("登录成功，需要刷新以获取uid");
            presenter.loadVideoUrl(unLimit91PornItem);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE || newConfig.orientation == ActivityInfo.SCREEN_ORIENTATION_USER) {
            //这里没必要，因为我们使用的是setColorForSwipeBack，并不会有这个虚拟的view，而是设置的padding
            StatusBarUtil.hideFakeStatusBarView(this);
        } else if (newConfig.orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimary));
        }
    }

    public void setV9PornItems(UnLimit91PornItem v9PornItems) {
        this.unLimit91PornItem = v9PornItems;
    }
}
