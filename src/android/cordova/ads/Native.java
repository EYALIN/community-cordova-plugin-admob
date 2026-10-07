package admob.plus.cordova.ads;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import admob.plus.cordova.ExecuteContext;
import admob.plus.cordova.Generated.Events;
import admob.plus.core.Context;

import static admob.plus.core.Helper.dpToPx;

public class Native extends AdBase {
    public static final String VIEW_DEFAULT_KEY = "default";
    public static final Map<String, ViewProvider> providers = new HashMap<>();

    private final AdRequest mAdRequest;
    private final ViewProvider viewProvider;
    private AdLoader mLoader;
    private NativeAd mAd;
    private View view;

    // Opt-in (default false): pre-1.3.0 behavior measured x/y from the window
    // top-left, which edge-to-edge (forced on API 35+) turns into the physical
    // screen top instead of the content area below the system bars. Apps that
    // already compensate for the status bar/cutout themselves must NOT opt in,
    // or the offset gets applied twice. Defaults to the old behavior for one
    // minor release; see README "Native ad position under edge-to-edge".
    private boolean applySystemBarInsets = false;
    private double lastRequestedX = 0.0;
    private double lastRequestedY = 0.0;
    private int lastTopInset = 0;
    private int lastLeftInset = 0;
    private OnApplyWindowInsetsListener insetsListener;

    public Native(ExecuteContext ctx) {
        super(ctx);

        mAdRequest = ctx.optAdRequest();
        String key = ctx.optString("view");
        if (key == null || key.isEmpty()) {
            key = VIEW_DEFAULT_KEY;
        }
        viewProvider = providers.get(key);
        if (viewProvider == null) {
            throw new RuntimeException("Cannot find viewProvider: " + key);
        }
    }

    @Override
    public void onDestroy() {
        clear();
        super.onDestroy();
    }

    @Override
    public boolean isLoaded() {
        return mAd != null;
    }

    @Override
    public void load(Context ctx) {
        clear();

        mLoader = new AdLoader.Builder(getActivity(), adUnitId)
                .forNativeAd(nativeAd -> {
                    // Destroy any existing ad before replacing it
                    if (mAd != null) {
                        mAd.destroy();
                    }
                    mAd = nativeAd;
                })
                .withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(LoadAdError adError) {
                        emit(Events.AD_LOAD_FAIL, adError);
                        mAd = null;
                        ctx.reject(adError.getCode() + ": " + adError.getMessage());
                    }

                    @Override
                    public void onAdLoaded() {
                        emit(Events.AD_LOAD);
                        ctx.resolve();
                    }

                    @Override
                    public void onAdOpened() {
                        emit(Events.AD_SHOW);
                    }

                    @Override
                    public void onAdClosed() {
                        emit(Events.AD_DISMISS);
                    }

                    @Override
                    public void onAdClicked() {
                        emit(Events.AD_CLICK);
                    }

                    @Override
                    public void onAdImpression() {
                        emit(Events.AD_IMPRESSION);
                    }
                })
                .build();

        mLoader.loadAd(mAdRequest);
    }

    @Override
    public void show(Context ctx) {
        if (mAd == null) {
            ctx.reject("Native ad not loaded");
            return;
        }

        Boolean applyInsetsOpt = ctx.optBoolean("applySystemBarInsets");
        applySystemBarInsets = applyInsetsOpt != null && applyInsetsOpt;

        if (view == null) {
            view = viewProvider.createView(mAd);
            Objects.requireNonNull(getContentView()).addView(view);
        }

        if (applySystemBarInsets && insetsListener == null) {
            attachInsetsListener();
        } else if (!applySystemBarInsets && insetsListener != null) {
            detachInsetsListener();
        }

        lastRequestedX = ctx.optDouble("x", 0.0);
        lastRequestedY = ctx.optDouble("y", 0.0);

        view.setVisibility(View.VISIBLE);
        applyPosition();

        ViewGroup.LayoutParams params = view.getLayoutParams();
        params.width = (int) dpToPx(ctx.optDouble("width", 0.0));
        params.height = (int) dpToPx(ctx.optDouble("height", 0.0));
        view.setLayoutParams(params);

        viewProvider.didShow(this);

        view.requestLayout();
        ctx.resolve(true);
    }

    /**
     * Applies the last requested x/y, adding the current system-bars + cutout
     * top/left inset when applySystemBarInsets is on, so the coordinates are
     * measured from the content area below the status bar (and right of any
     * left-edge cutout) rather than from the physical window top-left.
     */
    private void applyPosition() {
        if (view == null) return;

        int topInset = applySystemBarInsets ? lastTopInset : 0;
        int leftInset = applySystemBarInsets ? lastLeftInset : 0;

        view.setX((float) dpToPx(lastRequestedX) + leftInset);
        view.setY((float) dpToPx(lastRequestedY) + topInset);
    }

    private void attachInsetsListener() {
        ViewGroup contentView = getContentView();
        if (contentView == null) return;

        int mask = WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout();

        // Seed from the current insets so the first applyPosition() is already
        // correct instead of flashing at the uncorrected spot for a frame.
        WindowInsetsCompat current = ViewCompat.getRootWindowInsets(contentView);
        if (current != null) {
            Insets combined = current.getInsets(mask);
            lastTopInset = combined.top;
            lastLeftInset = combined.left;
        }

        insetsListener = (v, insets) -> {
            Insets combined = insets.getInsets(mask);
            lastTopInset = combined.top;
            lastLeftInset = combined.left;
            applyPosition();
            return insets;
        };
        ViewCompat.setOnApplyWindowInsetsListener(contentView, insetsListener);
        ViewCompat.requestApplyInsets(contentView);
    }

    private void detachInsetsListener() {
        ViewGroup contentView = getContentView();
        if (contentView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(contentView, null);
        }
        insetsListener = null;
        lastTopInset = 0;
        lastLeftInset = 0;
    }

    @Override
    public void hide(Context ctx) {
        if (view != null) {
            view.setVisibility(View.GONE);
        }

        viewProvider.didHide(this);
        ctx.resolve();
    }

    private void clear() {
        detachInsetsListener();

        if (mAd != null) {
            mAd.destroy();
            mAd = null;
        }
        if (view != null) {
            if (view instanceof NativeAdView) {
                NativeAdView v = (NativeAdView) view;
                v.removeAllViews();
                v.destroy();
            }
            view = null;
        }
        mLoader = null;
    }

    public interface ViewProvider {
        @NonNull
        View createView(NativeAd nativeAd);

        default void didShow(@NonNull Native ad) {}
        default void didHide(@NonNull Native ad) {}
    }
}
