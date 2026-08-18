import GoogleMobileAds
import UIKit

// MARK: - Banner Stack

class AMBBannerStackView: UIStackView {
    static let shared = AMBBannerStackView(frame: AMBHelper.window.frame)

    static let topConstraint = shared.topAnchor.constraint(equalTo: AMBHelper.topAnchor, constant: 0)
    static let bottomConstraint = shared.bottomAnchor.constraint(equalTo: AMBHelper.bottomAnchor, constant: 0)

    lazy var contentView: UIView = {
        let v = UIView(frame: self.frame)
        v.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        v.isUserInteractionEnabled = false
        return v
    }()

    var hasTopBanner: Bool {
        return self.arrangedSubviews.first is AMBBannerPlaceholder
    }

    var hasBottomBanner: Bool {
        return self.arrangedSubviews.last is AMBBannerPlaceholder
    }

    func prepare() {
        guard self.arrangedSubviews.isEmpty else { return }

        self.axis = .vertical
        self.distribution = .fill
        self.alignment = .fill
        self.translatesAutoresizingMaskIntoConstraints = false

        contentView.isUserInteractionEnabled = true
        self.addArrangedSubview(contentView)
    }


}

// MARK: - Banner

class AMBBanner: AMBAdBase, BannerViewDelegate, AdSizeDelegate {
    static let stackView = AMBBannerStackView.shared
    static let priortyLeast = UILayoutPriority(10)
    static var rootObservation: NSKeyValueObservation?
    static var marginTop: CGFloat?
    // Holds the dynamic "top == statusBar.bottom" constraint so updateLayout() can replace it
    // instead of stacking a new one on every root-view change (see updateLayout()).
    static var dynamicTopConstraint: NSLayoutConstraint?

    static var rootView: UIView {
        return AMBContext.plugin.viewController.view!
    }
    static var mainView: UIView {
        return AMBContext.plugin.webView
    }
    static var statusBarBackgroundView: UIView? {
        let statusBarFrame = UIApplication.shared.statusBarFrame
        return rootView.subviews.first { $0.frame.equalTo(statusBarFrame) }
    }
    /// Called from AMBPlugin.bannerConfig(...)
     static func config(_ ctx: AMBContext) {
       // 1) background color
       if let bg = ctx.optBackgroundColor() {
         rootView.backgroundColor = bg
       }

       // 2) top margin
       marginTop = ctx.optMarginTop()
       if let mt = marginTop {
         AMBBannerStackView.topConstraint.constant = mt
       }

       // 3) bottom margin
       if let mb = ctx.optMarginBottom() {
         AMBBannerStackView.bottomConstraint.constant = -mb
       }

       // 4) finally tell Cordova we're done
       ctx.resolve()
     }

    let adSize: AdSize!
    let position: String!
    let offset: CGFloat?
    private var bannerView: BannerView!
    private let placeholder = AMBBannerPlaceholder()

    init(id: String,
         adUnitId: String,
         adSize: AdSize,
         adRequest: Request,
         position: String,
         offset: CGFloat?) {
        self.adSize = adSize
        self.position = position
        self.offset = offset
        super.init(id: id, adUnitId: adUnitId, adRequest: adRequest)
    }

    convenience init?(_ ctx: AMBContext) {
        guard let id = ctx.optId(),
              let adUnitId = ctx.optAdUnitID()
        else { return nil }

        self.init(
            id: id,
            adUnitId: adUnitId,
            adSize: ctx.optAdSize(),
            adRequest: ctx.optRequest(),       // <-- updated here
            position: ctx.optPosition(),
            offset: ctx.optOffset()
        )
    }

    deinit {
        if bannerView != nil {
            bannerView.delegate = nil
            bannerView.adSizeDelegate = nil
            Self.stackView.removeArrangedSubview(placeholder)
            placeholder.removeFromSuperview()
            bannerView.removeFromSuperview()
            bannerView = nil
        }
    }

    override func isLoaded() -> Bool {
        return bannerView != nil
    }

    override func load(_ ctx: AMBContext) {
        if bannerView == nil {
            bannerView = BannerView(adSize: adSize)
            bannerView.delegate = self
            bannerView.adSizeDelegate = self
            bannerView.rootViewController = plugin.viewController
        }
        // Pin the banner to its FIXED ad height (e.g. LargeBanner = 100). This constraint is kept
        // active for the banner's whole life (it is NOT removed in bannerViewDidReceiveAd anymore).
        // Reason: the banner lives inside a full-bleed, distribution=.fill stack view. If the height
        // is left unconstrained, the fill stack stretches the banner to the whole screen (observed
        // height 840) or, mid-layout during boot, collapses it to 0 — which makes GMA reload with a
        // 0-height request and fail permanently with "Invalid ad width or height", leaving no banner.
        // A required height == the requested fixed ad size keeps it stable. (adSize.size.height is 0
        // only for adaptive sizes, which iOS never requests here; fall back to 50 defensively.)
        let pinnedHeight = adSize.size.height > 0 ? adSize.size.height : 50
        let fallbackHeightConstraint = bannerView.heightAnchor.constraint(equalToConstant: pinnedHeight)
        fallbackHeightConstraint.priority = .required
        fallbackHeightConstraint.isActive = true
        bannerView.adUnitID = adUnitId
        bannerView.load(adRequest)

        ctx.resolve()
    }

    override func show(_ ctx: AMBContext) {
        if let offset = offset {
            addBannerView(offset)
        } else {
            Self.prepareStackView()

            if position == AMBBannerPosition.top {
                Self.stackView.insertArrangedSubview(placeholder, at: 0)
            } else {
                Self.stackView.addArrangedSubview(placeholder)
            }
            placeholder.addSubview(bannerView)

            bannerView.translatesAutoresizingMaskIntoConstraints = false
            NSLayoutConstraint.activate([
                bannerView.topAnchor.constraint(equalTo: placeholder.topAnchor),
                bannerView.bottomAnchor.constraint(equalTo: placeholder.bottomAnchor),
                bannerView.leadingAnchor.constraint(equalTo: placeholder.leadingAnchor),
                bannerView.trailingAnchor.constraint(equalTo: placeholder.trailingAnchor),

            ])
        }

        bannerView.isHidden = false
        Self.updateLayout()
        ctx.resolve()
    }

    override func hide(_ ctx: AMBContext) {
        if bannerView != nil {
            bannerView.isHidden = true
            Self.stackView.removeArrangedSubview(placeholder)
            // removeArrangedSubview only stops the stack view from LAYING OUT the placeholder — the
            // view stays in the hierarchy with its last (top-strip) frame and keeps intercepting
            // touches OVER the WebView. That is the "menu/toolbar dead after a rewarded ad hides the
            // banner, only fixed by restart" bug (hitTest under the menu button returned
            // AMBBannerPlaceholder). Actually detach it from the view hierarchy.
            placeholder.removeFromSuperview()
            Self.updateLayout()
        }
        ctx.resolve()
    }

    // MARK: — Layout Helpers

    private static func prepareStackView() {
        guard stackView.arrangedSubviews.isEmpty else { return }

        stackView.prepare()
        stackView.translatesAutoresizingMaskIntoConstraints = false
        rootView.addSubview(stackView)

        NSLayoutConstraint.activate([
                stackView.topAnchor.constraint(equalTo: rootView.topAnchor),
                stackView.bottomAnchor.constraint(equalTo: rootView.bottomAnchor),
                stackView.leadingAnchor.constraint(equalTo: rootView.leadingAnchor),
                stackView.trailingAnchor.constraint(equalTo: rootView.trailingAnchor)
            ])

        rootObservation = rootView.observe(\.subviews, options: [.old, .new]) { _, _ in
            updateLayout()
        }
    }


    private static func updateLayout() {
        // This runs from a KVO observer on rootView.subviews, so it fires on EVERY view add/remove
        // (banner refresh, GMA internal views, full-screen ad present/dismiss…). The old code
        // activated a BRAND-NEW top constraint each time without deactivating the previous ones,
        // so conflicting constraints accumulated until Auto Layout collapsed the stack view — the
        // bottom banner vanished and the full-bleed stack could briefly black out the screen
        // ("blink"). Make it idempotent: always tear down the prior dynamic top constraint first.
        dynamicTopConstraint?.isActive = false
        dynamicTopConstraint = nil

        if let bar = statusBarBackgroundView,
           !bar.isHidden,
           rootView.subviews.contains(stackView)
        {
            AMBBannerStackView.topConstraint.isActive = false
            let c = stackView.topAnchor.constraint(equalTo: bar.bottomAnchor, constant: marginTop ?? 0)
            c.isActive = true
            dynamicTopConstraint = c
        } else {
            // <-- qualify the static constraints
            AMBBannerStackView.topConstraint.isActive = stackView.hasTopBanner
        }


        AMBBannerStackView.bottomConstraint.isActive = stackView.hasBottomBanner
    }

    private func addBannerView(_ offset: CGFloat) {

        let root = Self.rootView
        bannerView.translatesAutoresizingMaskIntoConstraints = false
        root.addSubview(bannerView)
        root.bringSubviewToFront(bannerView)
        var constraints = [
            bannerView.centerXAnchor.constraint(equalTo: root.centerXAnchor)
        ]
        if position == AMBBannerPosition.top {
            constraints.append(
                bannerView.topAnchor.constraint(equalTo: root.topAnchor, constant: offset)
            )
        } else {
            constraints.append(
                bannerView.bottomAnchor.constraint(equalTo: root.bottomAnchor, constant: -offset)
            )
        }
        NSLayoutConstraint.activate(constraints)
    }

    // MARK: — BannerViewDelegate

    func bannerViewDidReceiveAd(_ bannerView: BannerView) {
        // Intentionally do NOT remove the height constraint here. The old code removed it so the ad's
        // "natural" size could take over, but inside the fill stack view that let the banner stretch to
        // full-screen (840) or collapse to 0 → "Invalid ad width or height" → banner gone. For our
        // fixed-size iOS banner (LargeBanner) the pinned height IS the correct height, so keep it.
        Self.stackView.setNeedsLayout()
        Self.stackView.layoutIfNeeded()
        let info: [String: Any] = [
            "size": [
                "width": bannerView.frame.width,
                "height": bannerView.frame.height,
                "widthInPixels": round(bannerView.frame.width * UIScreen.main.scale),
                "heightInPixels": round(bannerView.frame.height * UIScreen.main.scale)
            ]
        ]
        emit(AMBEvents.adLoad, info)
        emit(AMBEvents.bannerLoad)
        emit(AMBEvents.bannerSize, info)
    }

    func bannerView(_ bannerView: BannerView,
                    didFailToReceiveAdWithError error: Error) {
        emit(AMBEvents.adLoadFail, error)
    }

    func bannerViewDidRecordImpression(_ bannerView: BannerView) {
        emit(AMBEvents.adImpression)
    }

    func bannerViewDidRecordClick(_ bannerView: BannerView) {
        emit(AMBEvents.adClick)
    }

    func bannerViewWillPresentScreen(_ bannerView: BannerView) {
        emit(AMBEvents.adShow)
    }

    func bannerViewWillDismissScreen(_ bannerView: BannerView) {
        // no-op
    }

    func bannerViewDidDismissScreen(_ bannerView: BannerView) {
        emit(AMBEvents.adDismiss)
    }

    // MARK: — AdSizeDelegate

    func adView(_ bannerView: BannerView, willChangeAdSizeTo size: AdSize) {
        emit(AMBEvents.bannerSizeChange, size)
    }
}
