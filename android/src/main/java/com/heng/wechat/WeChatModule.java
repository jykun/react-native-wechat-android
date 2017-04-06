package com.heng.wechat;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.text.TextUtils;
import android.util.Log;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.tencent.mm.opensdk.modelmsg.SendAuth;
import com.tencent.mm.opensdk.modelmsg.SendMessageToWX;
import com.tencent.mm.opensdk.modelmsg.WXImageObject;
import com.tencent.mm.opensdk.modelmsg.WXMediaMessage;
import com.tencent.mm.opensdk.modelmsg.WXMiniProgramObject;
import com.tencent.mm.opensdk.modelmsg.WXMusicObject;
import com.tencent.mm.opensdk.modelmsg.WXTextObject;
import com.tencent.mm.opensdk.modelmsg.WXVideoObject;
import com.tencent.mm.opensdk.modelmsg.WXWebpageObject;
import com.tencent.mm.opensdk.modelpay.PayReq;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * Created by heng on 2015/12/10.
 * <p/>
 * Edited by heng on 15/12/18
 * Added share webPage and weChat Pay
 * <p/>
 * Edited by heng on 2015/12/22
 * Add remote image async download
 * <p/>
 * Edited by heng on 2015/12/29
 * 1.Removed Handler and Thread
 * 2.Modify options param
 * 3.Added share local
 * 4.Added and remote image(分享远程图片到朋友圈和收藏都会失败,具体原因待查,建议把远程图片下载到本地来分享)
 * <p/>
 * Edited by heng on 2016/02/02
 * 1.Added method openWXApp
 * 2.Edited callback(err,res)
 * 3.Reconstruction code
 * <p/>
 * edited by kun on 2017/03/28
 * 1.added share mini program
 * 2.fixed share remote image
 * 3.updated wx sdk library
 */
public class WeChatModule extends ReactContextBaseJavaModule {

    public static final String REACT_MODULE_NAME = "WeChatAndroid";

    public static IWXAPI wxApi = null;
    public static String appId;
    public static ReactApplicationContext reactApplicationContext;


    /*============ WeChat share options key ==============*/
    public static final String OPTIONS_TITLE = "title";
    public static final String OPTIONS_DESC = "desc";
    public static final String OPTIONS_TAG_NAME = "tagName";
    public static final String OPTIONS_TRANSACTION = "transaction";
    public static final String OPTIONS_SCENE = "scene";
    public static final String OPTIONS_TYPE = "type";

    public static final String OPTIONS_TEXT = "text";

    public static final String OPTIONS_IMAGE_URL = "imageUrl";
    public static final String OPTIONS_IMAGE_PATH = "imagePath";

    public static final String OPTIONS_THUMB_IMAGE = "thumbImage";

    public static final String OPTIONS_WEBPAGE_URL = "webpageUrl";

    public static final String OPTIONS_MUSIC_URL = "musicUrl";
    public static final String OPTIONS_MUSIC_LOW_BAND_URL = "musicLowBandUrl";

    public static final String OPTIONS_VIDEO_URL = "videoUrl";
    public static final String OPTIONS_VIDEO_LOW_BAND_URL = "videoLowBandUrl";

    public static final String OPTIONS_PROGRAM_USER_NAME = "userName";
    public static final String OPTIONS_PROGRAM_PATH = "programPath";
    /*============ WeChat share options key ==============*/

    /*============ WeChat pay options key ==============*/
    public static final String OPTIONS_APP_ID = "appId";
    public static final String OPTIONS_NONCE_STR = "nonceStr";
    public static final String OPTIONS_PACKAGE_VALUE = "packageValue";
    public static final String OPTIONS_PARTNER_ID = "partnerId";
    public static final String OPTIONS_PREPAY_ID = "prepayId";
    public static final String OPTIONS_TIME_STAMP = "timeStamp";
    public static final String OPTIONS_SIGN = "sign";
    /*============ WeChat pay options key ==============*/


    public static final int TYPE_TEXT = 1;          //文字
    public static final int TYPE_IMAGE = 2;         //图片
    public static final int TYPE_WEB_PAGE = 3;      //网页
    public static final int TYPE_MUSIC = 4;         //音乐
    public static final int TYPE_VIDEO = 5;         //视频
    public static final int TYPE_PROGRAM = 6;       //小程序

    String tagName = null;
    String title = null;
    String desc = null;
    String transaction = null;
    Bitmap bitmap = null;           //分享的缩略图
    int scene;                      //分享的方式(0:聊天界面，1:朋友圈，2:收藏)


    public WeChatModule(ReactApplicationContext reactContext) {
        super(reactContext);
        reactApplicationContext = reactContext;
    }

    @Override
    public String getName() {
        return REACT_MODULE_NAME;
    }

    /**
     * 注册AppID到微信（使用微信SDK必须先调用此方法）
     */
    @ReactMethod
    public void registerApp(String appId, Callback callback) {
        if (TextUtils.isEmpty(appId)) {
            if (callback != null) {
                callback.invoke("appId must be not null !");
            }
        } else {
            WeChatModule.appId = appId;
            WeChatModule.wxApi = WXAPIFactory.createWXAPI(getReactApplicationContext(), appId, true);
            boolean registered = WeChatModule.wxApi.registerApp(appId);
            if (callback != null) {
                callback.invoke(null, registered);
            }
        }
    }

    private void commonCallback(Callback callback, boolean res) {
        if (callback != null) {
            if (WeChatModule.wxApi == null) {
                callback.invoke("please registerApp before this !");
                return;
            }
            callback.invoke(null, res);
        }
    }

    /**
     * 打开微信客户端
     */
    @ReactMethod
    public void openWXApp(Callback callback) {
        commonCallback(callback, WeChatModule.wxApi.openWXApp());
    }

    /**
     * 判断是否安装微信
     */
    @ReactMethod
    public void isWXAppInstalled(Callback callback) {
        commonCallback(callback, WeChatModule.wxApi.isWXAppInstalled());
    }

    /**
     * 判断安装的版本是否为微信支持的API
     */
    @ReactMethod
    public void isWXAppSupportAPI(Callback callback) {
        commonCallback(callback, WeChatModule.wxApi.isWXAppSupportAPI());
    }

    /**
     * 获取微信支持的API版本
     */
    @ReactMethod
    public void getWXAppSupportAPI(Callback callback) {
        if (callback != null) {
            if (WeChatModule.wxApi == null) {
                callback.invoke("please registerApp before this !");
                return;
            }
            int supportAPI = WeChatModule.wxApi.getWXAppSupportAPI();
            callback.invoke(null, supportAPI);
        }
    }

    /**
     * 微信授权登录
     */
    @ReactMethod
    public void sendAuthReq(String scope, String state, Callback callback) {
        if (WeChatModule.wxApi == null) {
            if (callback != null) {
                callback.invoke("please registerApp before this !");
            }
        } else {
            if (TextUtils.isEmpty(scope)) {
                scope = "snsapi_userinfo";
            }
            if (TextUtils.isEmpty(state)) {
                state = "SECRET";
            }
            SendAuth.Req req = new SendAuth.Req();
            req.scope = scope;
            req.state = state;
            boolean sendReqOK = WeChatModule.wxApi.sendReq(req);
            if (callback != null) {
                callback.invoke(null, sendReqOK);
            }
        }
    }

    /**
     * 分享到微信
     */
    @ReactMethod
    public void sendReq(ReadableMap options, Callback callback) {
        if (WeChatModule.wxApi == null) {
            if (callback != null) {
                callback.invoke("please registerApp before this !");
            }
        } else {
            if (options == null) {
                if (callback != null) {
                    callback.invoke("please setting options !");
                }
            } else {
                if (options.hasKey(OPTIONS_TYPE)) {
                    WXMediaMessage msg = new WXMediaMessage();

                    int type = options.getInt(OPTIONS_TYPE);
                    switch (type) {
                        case TYPE_TEXT:
                            msg.mediaObject = getTextObj(options);
                            break;
                        case TYPE_IMAGE:
                            msg.mediaObject = getImageObj(options);
                            break;
                        case TYPE_WEB_PAGE:
                            msg.mediaObject = getWebpageObj(options);
                            break;
                        case TYPE_MUSIC:
                            msg.mediaObject = getMusicObj(options);
                            break;
                        case TYPE_VIDEO:
                            msg.mediaObject = getVideoObj(options);
                            break;
                        case TYPE_PROGRAM:
                            msg.mediaObject = getProgramObject(options);
                            break;
                        default:
                            if (callback != null) {
                                callback.invoke("please check correct media type !");
                            }
                            break;
                    }

                    if (options.hasKey(OPTIONS_TITLE)) {
                        title = options.getString(OPTIONS_TITLE);
                    }
                    if (options.hasKey(OPTIONS_DESC)) {
                        desc = options.getString(OPTIONS_DESC);
                    }
                    if (options.hasKey(OPTIONS_TAG_NAME)) {
                        tagName = options.getString(OPTIONS_TAG_NAME);
                    }
                    if (options.hasKey(OPTIONS_TRANSACTION)) {
                        transaction = options.getString(OPTIONS_TRANSACTION);
                    }
                    if (options.hasKey(OPTIONS_SCENE)) {
                        scene = options.getInt(OPTIONS_SCENE);
                    }

                    if (!TextUtils.isEmpty(title)) {
                        msg.title = title;
                    }
                    if (!TextUtils.isEmpty(desc)) {
                        msg.description = desc;
                    }
                    if (!TextUtils.isEmpty(tagName)) {
                        msg.mediaTagName = tagName;
                    }
                    if (bitmap != null) {
                        msg.thumbData = compressBitmapToData(bitmap, 32);
                        while (msg.thumbData.length / 1024 > 32){
                            Matrix matrix = new Matrix();
                            matrix.setScale(0.5f, 0.5f);
                            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                            msg.thumbData = compressBitmapToData(bitmap, 32);
                        }
                        bitmap.recycle();
                    }

                    SendMessageToWX.Req req = new SendMessageToWX.Req();
                    req.message = msg;
                    if (!TextUtils.isEmpty(transaction)) {
                        req.transaction = transaction;
                    } else {
                        req.transaction = String.valueOf(System.currentTimeMillis());
                    }
                    if (scene == 0 || scene == 1 || scene == 2) {
                        req.scene = scene;
                    } else {
                        req.scene = 0;
                    }
                    boolean sendReqOK = WeChatModule.wxApi.sendReq(req);
                    if (callback != null) {
                        callback.invoke(null, sendReqOK);
                    }
                } else {
                    if (callback != null) {
                        callback.invoke("please setting share type !");
                    }
                }
            }
        }
    }


    /**
     * 微信支付
     */
    @ReactMethod
    public void weChatPay(ReadableMap options, Callback callback) {
        if (WeChatModule.wxApi == null) {
            if (callback != null) {
                callback.invoke("please registerApp before this !");
            }
            return;
        }
        String appId = WeChatModule.appId;
        String nonceStr = "";
        String packageValue = "";
        String partnerId = "";
        String prepayId = "";
        String timeStamp = "";
        String sign = "";
        if (options != null) {
            if (options.hasKey(OPTIONS_APP_ID)) {
                appId = options.getString(OPTIONS_APP_ID);
            }
            if (options.hasKey(OPTIONS_NONCE_STR)) {
                nonceStr = options.getString(OPTIONS_NONCE_STR);
            }
            if (options.hasKey(OPTIONS_PACKAGE_VALUE)) {
                packageValue = options.getString(OPTIONS_PACKAGE_VALUE);
            }
            if (options.hasKey(OPTIONS_PARTNER_ID)) {
                partnerId = options.getString(OPTIONS_PARTNER_ID);
            }
            if (options.hasKey(OPTIONS_PREPAY_ID)) {
                prepayId = options.getString(OPTIONS_PREPAY_ID);
            }
            if (options.hasKey(OPTIONS_TIME_STAMP)) {
                timeStamp = options.getString(OPTIONS_TIME_STAMP);
            }
            if (options.hasKey(OPTIONS_SIGN)) {
                sign = options.getString(OPTIONS_SIGN);
            }
        }

        PayReq request = new PayReq();
        request.appId = appId;
        request.nonceStr = nonceStr;
        request.packageValue = packageValue;
        request.partnerId = partnerId;
        request.prepayId = prepayId;
        request.timeStamp = timeStamp;
        request.sign = sign;
        boolean sendReqOK = WeChatModule.wxApi.sendReq(request);
        if (callback != null) {
            callback.invoke(null, sendReqOK);
        }
    }


    /**
     * 微信分享图片限制
     * @param bmp
     * @param size
     * @return
     */
    private byte[] compressBitmapToData(Bitmap bmp,float size) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] result;
        try {
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, output);//质量压缩方法，这里100表示不压缩，把压缩后的数据存放到baos中
            int options = 100;
            while ( output.toByteArray().length / 1024 >= size) {  //循环判断如果压缩后图片是否大于size kb,大于继续压缩
                output.reset();//重置baos即清空baos
                bmp.compress(Bitmap.CompressFormat.JPEG, options, output);//这里压缩options%，把压缩后的数据存放到baos中
                if(options==1){
                    break;
                }
                options -= 10;//每次都减少10
                if(options<=0){
                    options=1;
                }
            }
            result = output.toByteArray();
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }finally {
            try {
                output.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 获取文本对象
     * */
    private WXTextObject getTextObj(ReadableMap options) {
        WXTextObject textObject = new WXTextObject();
        if (options.hasKey(OPTIONS_TEXT)) {
            textObject.text = options.getString(OPTIONS_TEXT);
        }
        return textObject;
    }

    /**
     * 获取图片对象
     * */
    private WXImageObject getImageObj(ReadableMap options) {
        if (options.hasKey(OPTIONS_IMAGE_URL)) {
            String remoteUrl = options.getString(OPTIONS_IMAGE_URL);
            try {
                bitmap = BitmapFactory.decodeStream(new URL(remoteUrl).openStream());
            } catch (IOException e) {
                bitmap = null;
                e.printStackTrace();
            }
        }
        if (options.hasKey(OPTIONS_IMAGE_PATH)) {
            String localPath = options.getString(OPTIONS_IMAGE_PATH);
            if (localPath.contains("http")){
                try {
                    bitmap = BitmapFactory.decodeStream(new URL(localPath).openStream());
                } catch (IOException e) {
                    bitmap = null;
                    e.printStackTrace();
                }
            }else {
                File file = new File(localPath);
                if (file.exists()) {
                    bitmap = BitmapFactory.decodeFile(localPath);
                } else {
                    bitmap = null;
                }
            }
        }
        WXImageObject imageObject = new WXImageObject(bitmap);
        return imageObject;
    }

    /**
     * 获取网页对象
     * */
    private WXWebpageObject getWebpageObj(ReadableMap options) {
        WXWebpageObject webpageObject = new WXWebpageObject();
        if (options.hasKey(OPTIONS_WEBPAGE_URL)) {
            webpageObject.webpageUrl = options.getString(OPTIONS_WEBPAGE_URL);
        }
        if(options.hasKey(OPTIONS_THUMB_IMAGE)){
            String thumbImage = options.getString(OPTIONS_THUMB_IMAGE);
            try {
                bitmap = BitmapFactory.decodeStream(new URL(thumbImage).openStream());
            } catch (IOException e) {
                bitmap = null;
                e.printStackTrace();
            }
        }
        return webpageObject;
    }

    /**
     * 获取音乐对象
     * */
    private WXMusicObject getMusicObj(ReadableMap options) {
        WXMusicObject musicObject = new WXMusicObject();
        if (options.hasKey(OPTIONS_MUSIC_URL)) {
            musicObject.musicUrl = options.getString(OPTIONS_MUSIC_URL);
        }
        if (options.hasKey(OPTIONS_MUSIC_LOW_BAND_URL)) {
            musicObject.musicLowBandUrl = options.getString(OPTIONS_MUSIC_LOW_BAND_URL);
        }

        if(options.hasKey(OPTIONS_THUMB_IMAGE)){
            String thumbImage = options.getString(OPTIONS_THUMB_IMAGE);
            try {
                bitmap = BitmapFactory.decodeStream(new URL(thumbImage).openStream());
            } catch (IOException e) {
                bitmap = null;
                e.printStackTrace();
            }
        }
        return musicObject;
    }

    /**
     * 获取视频对象
     * */
    private WXVideoObject getVideoObj(ReadableMap options){
        WXVideoObject videoObject = new WXVideoObject();
        if(options.hasKey(OPTIONS_VIDEO_URL)){
            videoObject.videoUrl = options.getString(OPTIONS_VIDEO_URL);
        }
        if(options.hasKey(OPTIONS_VIDEO_LOW_BAND_URL)){
            videoObject.videoLowBandUrl = options.getString(OPTIONS_VIDEO_LOW_BAND_URL);
        }
        if(options.hasKey(OPTIONS_THUMB_IMAGE)){
            String thumbImage = options.getString(OPTIONS_THUMB_IMAGE);
            try {
                bitmap = BitmapFactory.decodeStream(new URL(thumbImage).openStream());
            } catch (IOException e) {
                bitmap = null;
                e.printStackTrace();
            }
        }
        return videoObject;
    }

    /*
    * 获取小程序对象
    * */
    private WXMiniProgramObject getProgramObject(ReadableMap options){
        WXMiniProgramObject programObject = new WXMiniProgramObject();
        if (options.hasKey(OPTIONS_WEBPAGE_URL)){
            programObject.webpageUrl = options.getString(OPTIONS_WEBPAGE_URL);// 低版本微信打开url
        }
        if (options.hasKey(OPTIONS_PROGRAM_USER_NAME)){
            programObject.userName = options.getString(OPTIONS_PROGRAM_USER_NAME); // 跳转微信小程序的原始name
        }
        if (options.hasKey(OPTIONS_PROGRAM_PATH)){
            // 小程序path
            programObject.path = options.getString(OPTIONS_PROGRAM_PATH);
        }

        if(options.hasKey(OPTIONS_THUMB_IMAGE)){
            String thumbImage = options.getString(OPTIONS_THUMB_IMAGE);
            try {
                bitmap = BitmapFactory.decodeStream(new URL(thumbImage).openStream());
            } catch (IOException e) {
                bitmap = null;
                e.printStackTrace();
            }
        }
        return programObject;
    }
}
