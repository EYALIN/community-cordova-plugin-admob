import { MobileAd, MobileAdOptions } from './shared';
export interface WebViewAdOptions extends MobileAdOptions {
    src?: string;
    adsense: string;
    npa?: '1';
}
export default class WebViewAd extends MobileAd<WebViewAdOptions> {
    private _loaded;
    private _src;
    private _adsense;
    private _originalHref;
    constructor(opts: WebViewAdOptions);
    addAd(opts: {
        element: HTMLElement;
        slot: string;
        format?: string;
        fullWidth?: boolean;
        html?: string;
    }): boolean;
    private nodeScriptReplace;
    private nodeScriptClone;
    private isNodeScript;
    private historyReplaceState;
    private historySetPage;
    private historyOriginalHref;
    private historyCurrentHref;
    private historyRestoreOriginalHref;
    show(): Promise<unknown>;
}
