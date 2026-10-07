import Foundation
import GoogleMobileAds
import UIKit

protocol AMBNativeAdViewProvider: NSObjectProtocol {
    func createView(_ nativeAd: NativeAd) -> UIView
    // delegate callbacks
    func didShow(_ ad: AMBNativeAd)
    func didHide(_ ad: AMBNativeAd)
}

extension AMBNativeAdViewProvider {
    func didShow(_ ad: AMBNativeAd) {}
    func didHide(_ ad: AMBNativeAd) {}
}

class AMBNativeAd: AMBAdBase, NativeAdLoaderDelegate, NativeAdDelegate {
    static var providers = [String: AMBNativeAdViewProvider]()

    private var mLoader: AdLoader!
    private let viewProvider: AMBNativeAdViewProvider
    private var mAd: NativeAd?
    private var ctxLoad: AMBContext?

    // Opt-in (default false): without it, x/y keep being measured from the
    // root view's top-left, same as before 1.3.0. The root view controller's
    // view already extends under the status bar / notch (UIKit does not
    // "edge-to-edge" the same way Android does, but the view's origin is
    // still behind the safe area), so a native ad positioned with a raw y can
    // land under the status bar/notch there too. Apps that already compensate
    // app-side must NOT opt in, or the offset is applied twice. See README
    // "Native ad position under edge-to-edge".
    private var applySystemBarInsets = false
    private var lastRequestedX: CGFloat = 0
    private var lastRequestedY: CGFloat = 0
    private var safeAreaObservation: NSKeyValueObservation?

    lazy var view: UIView = {
        return viewProvider.createView(mAd!)
    }()

    deinit {
        safeAreaObservation?.invalidate()
        safeAreaObservation = nil
    }

    init(
        id: String,
        adUnitId: String,
        adRequest: Request,
        viewProvider: AMBNativeAdViewProvider
    ) {
        self.viewProvider = viewProvider
        super.init(id: id, adUnitId: adUnitId, adRequest: adRequest)

        mLoader = AdLoader(
          adUnitID: adUnitId,
          rootViewController: plugin.viewController,
          adTypes: [.native],
          options: nil
        )
        mLoader.delegate = self
    }

    convenience init?(_ ctx: AMBContext) {
        let viewName = ctx.optString("view") ?? "default"
        guard
            let id = ctx.optId(),
            let adUnitId = ctx.optAdUnitID(),
            let provider = Self.providers[viewName]
        else {
            return nil
        }
        self.init(
          id: id,
          adUnitId: adUnitId,
          adRequest: ctx.optRequest(),
          viewProvider: provider
        )
    }

    override func load(_ ctx: AMBContext) {
        ctxLoad = ctx
        mLoader.load(adRequest)
    }

    override func isLoaded() -> Bool {
        return mLoader != nil && !mLoader.isLoading
    }

    override func show(_ ctx: AMBContext) {
        applySystemBarInsets = (ctx.opt("applySystemBarInsets") as? Bool) ?? false

        let root = plugin.viewController.view

        if
          let x = ctx.opt("x") as? Double,
          let y = ctx.opt("y") as? Double,
          let w = ctx.opt("width") as? Double,
          let h = ctx.opt("height") as? Double
        {
            lastRequestedX = CGFloat(x)
            lastRequestedY = CGFloat(y)
            view.frame = CGRect(x: 0, y: 0, width: CGFloat(w), height: CGFloat(h))
            applyPosition()
        }

        if
          let root = root,
          view.superview != root
        {
            root.addSubview(view)
        }

        if applySystemBarInsets {
            observeSafeArea(on: root)
        } else {
            safeAreaObservation?.invalidate()
            safeAreaObservation = nil
        }

        view.isHidden = false
        viewProvider.didShow(self)
    }

    /// Applies the last requested x/y, adding the root view's current top/left
    /// safe-area inset when applySystemBarInsets is on, restoring the
    /// pre-1.3.0 meaning of the coordinates (measured below the status bar /
    /// notch) instead of from the view's physical top-left.
    private func applyPosition() {
        let insets = applySystemBarInsets ? (plugin.viewController.view?.safeAreaInsets ?? .zero) : .zero
        view.frame.origin = CGPoint(
            x: lastRequestedX + insets.left,
            y: lastRequestedY + insets.top
        )
    }

    private func observeSafeArea(on root: UIView?) {
        guard safeAreaObservation == nil, let root = root else { return }

        safeAreaObservation = root.observe(\.safeAreaInsets, options: [.new]) { [weak self] _, _ in
            self?.applyPosition()
        }
    }

    override func hide(_ ctx: AMBContext) {
        view.isHidden = true
        viewProvider.didHide(self)
        ctx.resolve()
    }

    // MARK: - NativeAdLoaderDelegate

    func adLoader(_ adLoader: AdLoader, didReceive nativeAd: NativeAd) {
        mAd = nativeAd
        nativeAd.delegate = self
        emit(AMBEvents.adLoad)

        if !adLoader.isLoading {
            ctxLoad?.resolve()
            ctxLoad = nil
        }
    }

    func adLoader(_ adLoader: AdLoader, didFailToReceiveAdWithError error: Error) {
        emit(AMBEvents.adLoadFail, error)

        if !adLoader.isLoading {
            ctxLoad?.reject(error.localizedDescription)
            ctxLoad = nil
        }
    }

    // MARK: - NativeAdDelegate

    func nativeAdDidRecordImpression(_ nativeAd: NativeAd) {
        emit(AMBEvents.adImpression, nativeAd)
    }

    func nativeAdDidRecordClick(_ nativeAd: NativeAd) {
        emit(AMBEvents.adClick, nativeAd)
    }

    func nativeAdWillPresentScreen(_ nativeAd: NativeAd) {
        emit(AMBEvents.adShow, nativeAd)
    }

    func nativeAdWillDismissScreen(_ nativeAd: NativeAd) {
        // no-op
    }

    func nativeAdDidDismissScreen(_ nativeAd: NativeAd) {
        emit(AMBEvents.adDismiss, nativeAd)
    }

    func nativeAdWillLeaveApplication(_ nativeAd: NativeAd) {
        // no-op
    }
}
