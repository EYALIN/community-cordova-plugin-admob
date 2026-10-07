import { MobileAd, MobileAdOptions } from './shared'

type ShowOptions = {
  x: number
  y: number
  width: number
  height: number
  /**
   * Measure `x`/`y` from the content area below the system bars (status bar,
   * and the display cutout on Android) instead of from the window's physical
   * top-left corner.
   *
   * Default `false` for backward compatibility: under edge-to-edge (forced
   * from Android 15 / API 35 onward), the window spans the full screen, so a
   * native ad positioned with a raw `y` lands higher than before by the
   * status bar height unless the app already compensates for it itself. Set
   * this to `true` only if the app does NOT already apply its own
   * status-bar/inset correction to `y` (doing both double-corrects the
   * position).
   *
   * This will become the default in a future release — see the README.
   */
  applySystemBarInsets?: boolean
}

export interface NativeAdOptions extends MobileAdOptions {
  view?: string
}

export default class NativeAd extends MobileAd<NativeAdOptions> {
  static cls = 'NativeAd'

  public isLoaded() {
    return super.isLoaded()
  }

  async hide() {
    return super.hide()
  }

  public load() {
    return super.load()
  }

  async show(opts?: ShowOptions) {
    return super.show({
      x: 0,
      y: 0,
      width: 0,
      height: 0,
      applySystemBarInsets: false,
      ...opts,
    })
  }

  async showWith(
    elm: HTMLElement,
    opts?: Pick<ShowOptions, 'applySystemBarInsets'>,
  ) {
    const update = async () => {
      const r = elm.getBoundingClientRect()
      await this.show({
        x: r.x,
        y: r.y,
        width: r.width,
        height: r.height,
        ...opts,
      })
    }
    const observer = new MutationObserver(update)
    observer.observe(document.body, {
      attributes: true,
      childList: true,
      subtree: true,
    })
    document.addEventListener('scroll', update)
    window.addEventListener('resize', update)
    await update()
  }
}
