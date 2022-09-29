package com.github.tvbox.osc.util.js;

import android.content.Context;

import com.github.catvod.crawler.Spider;
import com.github.tvbox.quickjs.JSArray;
import com.github.tvbox.quickjs.JSObject;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class SpiderJS extends Spider {

    private String key;
    private String js;
    private String ext;
    private JSObject jsObject = null;

    public SpiderJS(String key, String js, String ext) {
        this.key = key;
        this.js = js;
        this.ext = ext;
    }

    void checkLoaderJS() {
        if (jsObject == null) {
            try {
                JSEngine.getInstance().postVoid(new Runnable() {
                    @Override
                    public void run() {
                        String moduleKey = "__" + UUID.randomUUID().toString().replace("-", "") + "__";
                        String jsContent = JSEngine.getInstance().loadModule(js);
                        jsContent = jsContent.replace("__JS_SPIDER__", "globalThis." + moduleKey);
                        JSEngine.getInstance().getJsContext().evaluateModule(jsContent, js);
                        jsObject = (JSObject) JSEngine.getInstance().getJsContext().getProperty(JSEngine.getInstance().getGlobalObj(), moduleKey);
                        jsObject.getJSFunction("init").call(ext);
                    }
                });
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }
    }

    String postFunc(String func, Object... args) {
        checkLoaderJS();
        if (jsObject != null) {
            try {
                return JSEngine.getInstance().post(() -> (String) jsObject.getJSFunction(func).call(args));
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }
        return "";
    }

    @Override
    public void init(Context context, String extend) {
        super.init(context, extend);
        checkLoaderJS();
    }

    @Override
    public String homeContent(boolean filter) {
        return postFunc("home", filter);
    }

    @Override
    public String homeVideoContent() {
        return postFunc("homeVod");
    }

    @Override
    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend) {
        try {
            JSObject obj = JSEngine.getInstance().post(() -> {
                JSObject o = JSEngine.getInstance().getJsContext().createNewJSObject();
                if (extend != null) {
                    for (String s : extend.keySet()) {
                        o.setProperty(s, extend.get(s));
                    }
                }
                return o;
            });
            return postFunc("category", tid, pg, filter, obj);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        return "";

    }

    @Override
    public String detailContent(List<String> ids) {
        return postFunc("detail", ids.get(0));
    }

    @Override
    public String playerContent(String flag, String id, List<String> vipFlags) {
        try {
            JSArray array = JSEngine.getInstance().post(() -> {
                JSArray arr = JSEngine.getInstance().getJsContext().createNewJSArray();
                if (vipFlags != null) {
                    for (int i = 0; i < vipFlags.size(); i++) {
                        arr.set(vipFlags.get(i), i);
                    }
                }
                return arr;
            });
            return postFunc("play", flag, id, array);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        return "";
    }

    @Override
    public String searchContent(String key, boolean quick) {
        return postFunc("search", key, quick);
    }
}
