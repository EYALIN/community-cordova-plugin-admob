"use strict";
var __extends = (this && this.__extends) || (function () {
    var extendStatics = function (d, b) {
        extendStatics = Object.setPrototypeOf ||
            ({ __proto__: [] } instanceof Array && function (d, b) { d.__proto__ = b; }) ||
            function (d, b) { for (var p in b) if (Object.prototype.hasOwnProperty.call(b, p)) d[p] = b[p]; };
        return extendStatics(d, b);
    };
    return function (d, b) {
        if (typeof b !== "function" && b !== null)
            throw new TypeError("Class extends value " + String(b) + " is not a constructor or null");
        extendStatics(d, b);
        function __() { this.constructor = d; }
        d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
    };
})();
var __assign = (this && this.__assign) || function () {
    __assign = Object.assign || function(t) {
        for (var s, i = 1, n = arguments.length; i < n; i++) {
            s = arguments[i];
            for (var p in s) if (Object.prototype.hasOwnProperty.call(s, p))
                t[p] = s[p];
        }
        return t;
    };
    return __assign.apply(this, arguments);
};
var __awaiter = (this && this.__awaiter) || function (thisArg, _arguments, P, generator) {
    function adopt(value) { return value instanceof P ? value : new P(function (resolve) { resolve(value); }); }
    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
        function rejected(value) { try { step(generator["throw"](value)); } catch (e) { reject(e); } }
        function step(result) { result.done ? resolve(result.value) : adopt(result.value).then(fulfilled, rejected); }
        step((generator = generator.apply(thisArg, _arguments || [])).next());
    });
};
var __generator = (this && this.__generator) || function (thisArg, body) {
    var _ = { label: 0, sent: function() { if (t[0] & 1) throw t[1]; return t[1]; }, trys: [], ops: [] }, f, y, t, g;
    return g = { next: verb(0), "throw": verb(1), "return": verb(2) }, typeof Symbol === "function" && (g[Symbol.iterator] = function() { return this; }), g;
    function verb(n) { return function (v) { return step([n, v]); }; }
    function step(op) {
        if (f) throw new TypeError("Generator is already executing.");
        while (_) try {
            if (f = 1, y && (t = op[0] & 2 ? y["return"] : op[0] ? y["throw"] || ((t = y["return"]) && t.call(y), 0) : y.next) && !(t = t.call(y, op[1])).done) return t;
            if (y = 0, t) op = [op[0] & 2, t.value];
            switch (op[0]) {
                case 0: case 1: t = op; break;
                case 4: _.label++; return { value: op[1], done: false };
                case 5: _.label++; y = op[1]; op = [0]; continue;
                case 7: op = _.ops.pop(); _.trys.pop(); continue;
                default:
                    if (!(t = _.trys, t = t.length > 0 && t[t.length - 1]) && (op[0] === 6 || op[0] === 2)) { _ = 0; continue; }
                    if (op[0] === 3 && (!t || (op[1] > t[0] && op[1] < t[3]))) { _.label = op[1]; break; }
                    if (op[0] === 6 && _.label < t[1]) { _.label = t[1]; t = op; break; }
                    if (t && _.label < t[2]) { _.label = t[2]; _.ops.push(op); break; }
                    if (t[2]) _.ops.pop();
                    _.trys.pop(); continue;
            }
            op = body.call(thisArg, _);
        } catch (e) { op = [6, e]; y = 0; } finally { f = t = 0; }
        if (op[0] & 5) throw op[1]; return { value: op[0] ? op[1] : void 0, done: true };
    }
};
Object.defineProperty(exports, "__esModule", { value: true });
var shared_1 = require("./shared");
var WebViewAd = /** @class */ (function (_super) {
    __extends(WebViewAd, _super);
    function WebViewAd(opts) {
        var _this = this;
        var _a, _b, _c, _d, _e, _f, _g;
        opts.adUnitId = '';
        _this = _super.call(this, opts) || this;
        _this._loaded = false;
        _this._src = '';
        _this._adsense = '';
        _this._originalHref = window.location.href || '';
        _this._adsense = opts.adsense;
        _this._src = opts.src || 'https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js';
        if (typeof ((_a = window.gmaSdk) === null || _a === void 0 ? void 0 : _a.getQueryInfo) === 'function' || typeof ((_d = (_c = (_b = window.webkit) === null || _b === void 0 ? void 0 : _b.messageHandlers) === null || _c === void 0 ? void 0 : _c.getGmaQueryInfo) === null || _d === void 0 ? void 0 : _d.postMessage) === 'function' || typeof ((_g = (_f = (_e = window.webkit) === null || _e === void 0 ? void 0 : _e.messageHandlers) === null || _f === void 0 ? void 0 : _f.getGmaSig) === null || _g === void 0 ? void 0 : _g.postMessage) === 'function') {
            var html = '<script async src="' + _this._src + '" crossorigin="anonymous"></script>';
            if (opts.npa) {
                html += '<script>(window.adsbygoogle = window.adsbygoogle || []).requestNonPersonalizedAds = 1</script>';
            }
            html += '<script>(window.adsbygoogle = window.adsbygoogle || []).push({google_ad_client: "' + _this._adsense + '", enable_page_level_ads: true, overlays: false});</script>';
            var div = document.createElement('div');
            div.innerHTML = html;
            document.head.appendChild(div);
            _this.nodeScriptReplace(div);
            _this._loaded = true;
        }
        else {
            console.error('WebView does not appear to be setup correctly');
        }
        return _this;
    }
    WebViewAd.prototype.addAd = function (opts) {
        opts = __assign({ format: 'auto', fullWidth: true }, opts);
        if (this._loaded) {
            var html = opts.html || '';
            if (!opts.html) {
                html = '<script async src="' + this._src + '" crossorigin="anonymous"></script>';
                html += '<ins class="adsbygoogle" style="display:block" data-ad-client="' + this._adsense + '" data-ad-slot="' + opts.slot + '" data-ad-format="' + opts.format + '" data-full-width-responsive="' + (opts.fullWidth ? 'true' : 'false') + '"></ins>';
                html += '<script>(window.adsbygoogle = window.adsbygoogle || []).push({});</script>';
            }
            if (opts.element) {
                opts.element.innerHTML = html;
                this.nodeScriptReplace(opts.element);
                return true;
            }
        }
        return false;
    };
    WebViewAd.prototype.nodeScriptReplace = function (node) {
        if (this.isNodeScript(node) === true) {
            node.parentNode.replaceChild(this.nodeScriptClone(node), node);
        }
        else {
            var children = node.childNodes;
            for (var i = 0, len = children.length; i < len; i++) {
                this.nodeScriptReplace(children[i]);
            }
        }
        return node;
    };
    WebViewAd.prototype.nodeScriptClone = function (node) {
        var script = document.createElement('script');
        script.text = node.innerHTML;
        var attrs = node.attributes;
        for (var i = 0, len = attrs.length; i < len; i++) {
            script.setAttribute(attrs[i].name, attrs[i].value);
        }
        return script;
    };
    WebViewAd.prototype.isNodeScript = function (node) {
        return node.tagName === 'SCRIPT';
    };
    WebViewAd.prototype.historyReplaceState = function (url) {
        if (!this._originalHref) {
            this._originalHref = window.location.href;
        }
        if (this._loaded) {
            window.history.replaceState(null, '', url);
        }
    };
    WebViewAd.prototype.historySetPage = function (page, parameters) {
        if (parameters === void 0) { parameters = {}; }
        var _parameters = [];
        for (var name_1 in parameters) {
            _parameters.push(name_1 + '=' + encodeURI(parameters[name_1]));
        }
        var url = page + (_parameters.length > 0 ? '?' + _parameters.join('&') : '');
        this.historyReplaceState(url);
        return url;
    };
    WebViewAd.prototype.historyOriginalHref = function () {
        return this._originalHref || window.location.href;
    };
    WebViewAd.prototype.historyCurrentHref = function () {
        return window.location.href;
    };
    WebViewAd.prototype.historyRestoreOriginalHref = function () {
        this.historyReplaceState(this.historyOriginalHref());
    };
    WebViewAd.prototype.show = function () {
        return __awaiter(this, void 0, void 0, function () {
            return __generator(this, function (_a) {
                switch (_a.label) {
                    case 0:
                        if (!!this._loaded) return [3 /*break*/, 2];
                        return [4 /*yield*/, this.load()];
                    case 1:
                        _a.sent();
                        _a.label = 2;
                    case 2: return [2 /*return*/, _super.prototype.show.call(this)];
                }
            });
        });
    };
    return WebViewAd;
}(shared_1.MobileAd));
exports.default = WebViewAd;
//# sourceMappingURL=webview.js.map