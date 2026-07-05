package admob.plus.cordova.ads;

import android.app.Activity;
import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.graphics.Color;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnPreDrawListener;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;

import java.util.HashMap;

import admob.plus.cordova.ExecuteContext;
import admob.plus.cordova.Generated.Events;
import admob.plus.core.Context;

import static admob.plus.core.Helper.getParentView;
import static admob.plus.core.Helper.pxToDp;
import static admob.plus.core.Helper.removeFromParentView;

public class Banner extends AdBase {
    private static final String TAG = "AdMobPlus.Banner";

    @SuppressLint("StaticFieldLeak")
    private static ViewGroup rootLinearLayout;

    @SuppressLint("StaticFieldLeak")
    private static ViewGroup originalWebViewParent;

    @SuppressLint("StaticFieldLeak")
    private static FrameLayout webViewFrame;

    @SuppressLint("StaticFieldLeak")
    private static FrameLayout bannerSlot;

    private static int screenWidth = 0;

    private final AdSize adSize;
    private final int gravity;
    private final Integer offset;

    private AdView mAdView;
    private RelativeLayout mRelativeLayout = null;
    private AdView mAdViewOld = null;

    private boolean hidden = false;
    private boolean linearBannerVisible = false;

    private int lastTopInset = 0;
    private int lastBottomInset = 0;
    private int lastRelativeTopInset = 0;
    private int lastRelativeBottomInset = 0;

    private boolean pendingShowAfterOrientationChange = false;
    private boolean suppressBannerDuringOrientationChange = false;

    public Banner(ExecuteContext ctx) {
        super(ctx);

        this.adSize = ctx.optAdSize();
        this.gravity = "top".equals(ctx.optPosition()) ? Gravity.TOP : Gravity.BOTTOM;
        this.offset = ctx.optOffset();
    }

    public static void destroyParentView() {
        ViewGroup vg = getParentView(rootLinearLayout);
        if (vg != null) vg.removeAllViews();

        rootLinearLayout = null;
        originalWebViewParent = null;
        webViewFrame = null;
        bannerSlot = null;
    }

    private static void runJustBeforeBeingDrawn(final View view, final Runnable runnable) {
        final OnPreDrawListener preDrawListener = new OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                view.getViewTreeObserver().removeOnPreDrawListener(this);
                runnable.run();
                return true;
            }
        };
        view.getViewTreeObserver().addOnPreDrawListener(preDrawListener);
    }

    @Override
    public boolean isLoaded() {
        return mAdView != null;
    }

    @Override
    public void load(Context ctx) {
        if (mAdView == null) {
            mAdView = createBannerView();
        }

        if (offset == null) {
            destroyRelativeLayoutMode();
            ensureLinearBannerLayout();
        }

        mAdView.loadAd(adRequest);
        ctx.resolve();
    }

    private AdView createBannerView() {
        AdView adView = new AdView(getActivity());
        adView.setAdUnitId(adUnitId);
        adView.setAdSize(adSize);
        adView.setFocusable(false);
        adView.setFocusableInTouchMode(false);
        adView.setBackgroundColor(Color.TRANSPARENT);

        adView.setAdListener(new AdListener() {
            @Override
            public void onAdClicked() {
                emit(Events.AD_CLICK);
            }

            @Override
            public void onAdClosed() {
                emit(Events.AD_DISMISS);
            }

            @Override
            public void onAdFailedToLoad(LoadAdError error) {
                emit(Events.AD_LOAD_FAIL, error);
            }

            @Override
            public void onAdImpression() {
                emit(Events.AD_IMPRESSION);
            }

            @Override
            public void onAdLoaded() {
                if (offset == null && (linearBannerVisible || mAdViewOld != null)) {
                    addBannerViewWithLinearLayout();
                } else if (offset != null && adView == mAdView && mAdView.getParent() == null) {
                    addBannerViewWithRelativeLayout();
                }

                if (mAdViewOld != null) {
                    removeBannerView(mAdViewOld);
                    mAdViewOld = null;
                }

                if (offset == null) {
                    applyBannerSlotLayout();
                }

                runJustBeforeBeingDrawn(adView, () -> emit(Events.BANNER_SIZE, computeAdSize()));
                emit(Events.AD_LOAD, computeAdSize());
            }

            @Override
            public void onAdOpened() {
                emit(Events.AD_SHOW);
            }
        });

        return adView;
    }

    @NonNull
    private HashMap<String, Object> computeAdSize() {
        int width = mAdView.getWidth();
        int height = mAdView.getHeight();

        return new HashMap<String, Object>() {{
            put("size", new HashMap<String, Object>() {{
                put("width", pxToDp(width));
                put("height", pxToDp(height));
                put("widthInPixels", width);
                put("heightInPixels", height);
            }});
        }};
    }

    @Override
    public void show(Context ctx) {
        hidden = false;

        if (mAdView == null) {
            mAdView = createBannerView();
            mAdView.loadAd(adRequest);
        }

        resumeBannerViews();

        addBannerView();
        ctx.resolve();
    }

    @Override
    public void hide(Context ctx) {
        hidden = true;

        if (mAdView != null) {
            pauseBannerViews();

            if (offset == null) {
                linearBannerVisible = false;
                hideBannerLinearLayout();
            } else {
                mAdView.setVisibility(View.GONE);
            }
        }

        ctx.resolve();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        int w = getActivity().getResources().getDisplayMetrics().widthPixels;
        if (w != screenWidth) {
            screenWidth = w;

            getActivity().runOnUiThread(() -> {
                 if (hidden) {
                    pendingShowAfterOrientationChange = false;
                    suppressBannerDuringOrientationChange = false;
                    return;
                }

                pendingShowAfterOrientationChange =
                        mAdView != null &&
                        mAdView.getVisibility() == View.VISIBLE;

                suppressBannerDuringOrientationChange = true;

                if (mAdView != null) {
                    mAdView.setVisibility(View.INVISIBLE);
                }

                if (offset == null) {
                    pendingShowAfterOrientationChange = false;
                    suppressBannerDuringOrientationChange = false;
                    mAdView.setVisibility(View.VISIBLE);

                    ensureLinearBannerLayout();
                    applyBannerSlotLayout();
                }

                reloadBannerView();

                View root = offset == null ? rootLinearLayout : mRelativeLayout;
                if (root != null) {
                    root.postDelayed(() -> {
                        suppressBannerDuringOrientationChange = false;

                        if (pendingShowAfterOrientationChange && mAdView != null) {
                            if (offset == null) {
                                addBannerViewWithLinearLayout();
                            } else {
                                applyRelativeBannerMargins();
                                mAdView.setVisibility(View.VISIBLE);
                            }
                        }

                        pendingShowAfterOrientationChange = false;
                    }, 150);
                }
            });
        }
    }

    private void reloadBannerView() {
        if (hidden)
            return;
        
        if (mAdView == null || mAdView.getVisibility() == View.GONE)
            return;

        pauseBannerViews();

        if (mAdViewOld != null) {
            removeBannerView(mAdViewOld);
        }

        mAdViewOld = mAdView;
        mAdView = createBannerView();
        mAdView.loadAd(adRequest);

        if (offset != null) {
            addBannerView();
        }
    }

    @Override
    public void onPause(boolean multitasking) {
        pauseBannerViews();
        super.onPause(multitasking);
    }

    private void pauseBannerViews() {
        if (mAdView != null) mAdView.pause();
        if (mAdViewOld != null && mAdViewOld != mAdView) {
            mAdViewOld.pause();
        }
    }

    @Override
    public void onResume(boolean multitasking) {
        super.onResume(multitasking);
        resumeBannerViews();
    }

    private void resumeBannerViews() {
        if (mAdView != null) mAdView.resume();
        if (mAdViewOld != null) mAdViewOld.resume();
    }

    @Override
    public void onDestroy() {
        resetLinearBannerLayout();

        if (mAdView != null) {
            removeBannerView(mAdView);
            mAdView = null;
        }

        if (mAdViewOld != null) {
            removeBannerView(mAdViewOld);
            mAdViewOld = null;
        }

        destroyRelativeLayoutMode();

        super.onDestroy();
    }

    private void removeBannerView(@NonNull AdView adView) {
        removeFromParentView(adView);
        adView.removeAllViews();
        adView.destroy();
    }

    private void addBannerView() {
        if (mAdView == null)
            return;

        if (this.offset == null) {
            destroyRelativeLayoutMode();
            addBannerViewWithLinearLayout();
        } else {
            linearBannerVisible = false;
            resetLinearBannerLayout();

            if (getParentView(mAdView) == mRelativeLayout && mRelativeLayout != null)
                return;
            addBannerViewWithRelativeLayout();
        }

        ViewGroup contentView = getContentView();
        if (contentView != null) {
            contentView.bringToFront();
            contentView.requestLayout();
            contentView.requestFocus();
        }
    }

    private void ensureLinearBannerLayout() {
        View webView = getWebView();

        if (rootLinearLayout != null && bannerSlot != null && webViewFrame != null) {
            return;
        }

        ViewGroup wvParentView = getParentView(webView);
        if (wvParentView == null)
            return;

        if (rootLinearLayout != null) {
            resetLinearBannerLayout();
            wvParentView = getParentView(webView);
            if (wvParentView == null)
                return;
        }

        originalWebViewParent = wvParentView;

        rootLinearLayout = new LinearLayout(getActivity());
        ((LinearLayout) rootLinearLayout).setOrientation(LinearLayout.VERTICAL);
        rootLinearLayout.setBackgroundColor(Color.TRANSPARENT);

        webViewFrame = new FrameLayout(getActivity());
        webViewFrame.setBackgroundColor(Color.TRANSPARENT);

        bannerSlot = new FrameLayout(getActivity());
        bannerSlot.setBackgroundColor(Color.TRANSPARENT);
        bannerSlot.setVisibility(View.GONE);

        wvParentView.removeView(webView);

        webViewFrame.addView(webView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        ViewCompat.setOnApplyWindowInsetsListener(webViewFrame, (view, insets) -> {
            clearWebViewInsetMargin(webView);
            return insets;
        });

        if (isPositionTop()) {
            rootLinearLayout.addView(bannerSlot, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    0
            ));

            rootLinearLayout.addView(webViewFrame, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    0,
                    1.0F
            ));
        } else {
            rootLinearLayout.addView(webViewFrame, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    0,
                    1.0F
            ));

            rootLinearLayout.addView(bannerSlot, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    0
            ));
        }

        wvParentView.addView(rootLinearLayout, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        ViewCompat.setOnApplyWindowInsetsListener(rootLinearLayout, (view, insets) -> {
            lastTopInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            lastBottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;

            view.post(() -> {
                applyBannerSlotLayout();
                clearWebViewInsetMargin(webView);
            });

            return insets;
        });

        rootLinearLayout.post(() -> {
            ViewCompat.requestApplyInsets(rootLinearLayout);

            rootLinearLayout.postDelayed(() -> {
                applyBannerSlotLayout();
                clearWebViewInsetMargin(webView);
                rootLinearLayout.requestLayout();
            }, 100);
        });
    }

    private void addBannerViewWithLinearLayout() {
        ensureLinearBannerLayout();

        if (bannerSlot == null || mAdView == null)
            return;

        linearBannerVisible = true;

        removeFromParentView(mAdView);
        bannerSlot.removeAllViews();

        mAdView.setVisibility(
            suppressBannerDuringOrientationChange ? View.INVISIBLE : View.VISIBLE
        );

        mAdView.setFocusable(false);
        mAdView.setFocusableInTouchMode(false);
        mAdView.setBackgroundColor(Color.TRANSPARENT);

        bannerSlot.addView(mAdView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        bannerSlot.setVisibility(View.VISIBLE);
        applyBannerSlotLayout();

        if (rootLinearLayout != null) {
            rootLinearLayout.post(() -> {
                ViewCompat.requestApplyInsets(rootLinearLayout);

                rootLinearLayout.postDelayed(() -> {
                    applyBannerSlotLayout();
                    clearWebViewInsetMargin(getWebView());
                    rootLinearLayout.requestLayout();
                }, 100);
            });
        }
    }

    private void hideBannerLinearLayout() {
        if (bannerSlot == null)
            return;

        removeFromParentView(mAdView);
        bannerSlot.removeAllViews();
        bannerSlot.setVisibility(View.GONE);

        ViewGroup.LayoutParams lp = bannerSlot.getLayoutParams();
        if (lp instanceof LinearLayout.LayoutParams) {
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) lp;
            params.height = 0;
            params.setMargins(0, 0, 0, 0);
            bannerSlot.setLayoutParams(params);
        }
    }

    private void applyBannerSlotLayout() {
        if (bannerSlot == null || mAdView == null || !linearBannerVisible)
            return;

        int adHeightPx = mAdView.getAdSize() != null
                ? mAdView.getAdSize().getHeightInPixels(getActivity())
                : adSize.getHeightInPixels(getActivity());

        ViewGroup.LayoutParams slotLp = bannerSlot.getLayoutParams();
        if (slotLp instanceof LinearLayout.LayoutParams) {
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) slotLp;

            params.height = adHeightPx;

            if (isPositionTop()) {
                params.setMargins(0, lastTopInset, 0, 0);
            } else {
                params.setMargins(0, 0, 0, lastBottomInset);
            }

            bannerSlot.setLayoutParams(params);
            bannerSlot.requestLayout();
        }
    }

    private void clearWebViewInsetMargin(View webView) {
        ViewGroup.LayoutParams webLp = webView.getLayoutParams();

        if (webLp instanceof FrameLayout.LayoutParams) {
            FrameLayout.LayoutParams webParams = (FrameLayout.LayoutParams) webLp;

            if (isPositionTop()) {
                webParams.setMargins(
                        webParams.leftMargin,
                        0,
                        webParams.rightMargin,
                        webParams.bottomMargin
                );
            } else {
                webParams.setMargins(
                        webParams.leftMargin,
                        webParams.topMargin,
                        webParams.rightMargin,
                        0
                );
            }

            webView.setLayoutParams(webParams);
            webView.requestLayout();
        }
    }

    private void resetLinearBannerLayout() {
        View webView = getWebView();

        linearBannerVisible = false;
        removeFromParentView(mAdView);

        if (bannerSlot != null) {
            bannerSlot.removeAllViews();
            bannerSlot = null;
        }

        if (webViewFrame != null) {
            removeFromParentView(webView);
            webViewFrame.removeAllViews();
            webViewFrame = null;
        }

        if (originalWebViewParent != null && getParentView(webView) != originalWebViewParent) {
            removeFromParentView(webView);

            originalWebViewParent.addView(webView, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            ));
        }

        removeFromParentView(rootLinearLayout);
        rootLinearLayout = null;
        originalWebViewParent = null;
    }

    private void destroyRelativeLayoutMode() {
        if (mRelativeLayout != null) {
            removeFromParentView(mRelativeLayout);
            mRelativeLayout = null;
        }
    }

    private void addBannerViewWithRelativeLayout() {
        if (mRelativeLayout == null) {
            mRelativeLayout = new RelativeLayout(getActivity());
            mRelativeLayout.setFocusable(false);
            mRelativeLayout.setFocusableInTouchMode(false);
            mRelativeLayout.setClickable(false);

            ViewGroup contentView = getContentView();
            if (contentView != null) {
                contentView.addView(mRelativeLayout, new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                ));
            } else {
                Log.e(TAG, "Unable to find content view");
            }
        }

        removeFromParentView(mAdView);

        mAdView.setVisibility(View.VISIBLE);
        mAdView.setFocusable(false);
        mAdView.setFocusableInTouchMode(false);

        RelativeLayout.LayoutParams paramsContent = new RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        );

        if (isPositionTop()) {
            paramsContent.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            paramsContent.setMargins(0, this.offset, 0, 0);
        } else {
            paramsContent.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            paramsContent.setMargins(0, 0, 0, this.offset);
        }

        mRelativeLayout.addView(mAdView, paramsContent);
        mRelativeLayout.bringToFront();

        ViewCompat.setOnApplyWindowInsetsListener(mRelativeLayout, (view, insets) -> {
            lastRelativeTopInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            lastRelativeBottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;

            view.post(() -> {
                applyRelativeBannerMargins();

                if (!suppressBannerDuringOrientationChange && pendingShowAfterOrientationChange) {
                    mAdView.setVisibility(View.VISIBLE);
                    pendingShowAfterOrientationChange = false;
                }
            });

            return insets;
        });

        mRelativeLayout.post(() -> {
            ViewCompat.requestApplyInsets(mRelativeLayout);

            mRelativeLayout.postDelayed(() -> {
                ViewCompat.requestApplyInsets(mRelativeLayout);
                mAdView.requestLayout();
            }, 100);
        });
    }

    private void applyRelativeBannerMargins() {
        if (mAdView == null)
            return;

        ViewGroup.LayoutParams lp = mAdView.getLayoutParams();
        if (!(lp instanceof RelativeLayout.LayoutParams))
            return;

        RelativeLayout.LayoutParams adParams = (RelativeLayout.LayoutParams) lp;

        if (isPositionTop()) {
            adParams.setMargins(0, lastRelativeTopInset + this.offset, 0, 0);
        } else {
            adParams.setMargins(0, 0, 0, lastRelativeBottomInset + this.offset);
        }

        mAdView.setLayoutParams(adParams);
        mAdView.requestLayout();
    }

    private boolean isPositionTop() {
        return gravity == Gravity.TOP;
    }

    public enum AdSizeType {
        BANNER,
        LARGE_BANNER,
        MEDIUM_RECTANGLE,
        FULL_BANNER,
        LEADERBOARD,
        ADAPTIVE;

        @Nullable
        public static AdSize getAdSize(int adSize, @NonNull Activity activity) {
            switch (AdSizeType.values()[adSize]) {
                case BANNER:
                    return AdSize.BANNER;
                case LARGE_BANNER:
                    return AdSize.LARGE_BANNER;
                case MEDIUM_RECTANGLE:
                    return AdSize.MEDIUM_RECTANGLE;
                case FULL_BANNER:
                    return AdSize.FULL_BANNER;
                case LEADERBOARD:
                    return AdSize.LEADERBOARD;
                case ADAPTIVE:
                    int adWidth = getAdWidthInPixels(activity);
                    int adWidthDp = (int) (adWidth / activity.getResources().getDisplayMetrics().density);
                    return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidthDp);
                default:
                    return AdSize.BANNER;
            }
        }

        private static int getAdWidthInPixels(@NonNull Activity activity) {
            return activity.getResources().getDisplayMetrics().widthPixels;
        }
    }
}