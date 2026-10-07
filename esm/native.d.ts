import { MobileAd, MobileAdOptions } from './shared';
declare type ShowOptions = {
    x: number;
    y: number;
    width: number;
    height: number;
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
    applySystemBarInsets?: boolean;
};
export interface NativeAdOptions extends MobileAdOptions {
    view?: string;
}
export default class NativeAd extends MobileAd<NativeAdOptions> {
    static cls: string;
    isLoaded(): Promise<boolean>;
    hide(): Promise<unknown>;
    load(): Promise<void>;
    show(opts?: ShowOptions): Promise<unknown>;
    showWith(elm: HTMLElement, opts?: Pick<ShowOptions, 'applySystemBarInsets'>): Promise<void>;
}
export {};
