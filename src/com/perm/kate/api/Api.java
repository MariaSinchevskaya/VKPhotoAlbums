package com.perm.kate.api;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.zip.GZIPInputStream;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.perm.utils.Utils;
import com.perm.utils.WrongResponseCodeException;
import android.util.Log;

public class Api {
    static final String TAG="Kate.Api";
    
    public static final String BASE_URL="https://api.vk.com/method/";
    
    public Api(String access_token, String api_id){
        this.access_token=access_token;
        this.api_id=api_id;
    }
    
    String access_token;
    String api_id;
    
    //TODO: it's not faster, even slower on slow devices. Maybe we should add an option to disable it. It's only good for paid internet connection.
    static boolean enable_compression=true;
    
    /*** utils methods***/
    private void checkError(JSONObject root, String url) throws JSONException,KException {
        if(!root.isNull("error")){
            JSONObject error=root.getJSONObject("error");
            int code=error.getInt("error_code");
            String message=error.getString("error_msg");
            KException e = new KException(code, message, url); 
            if (code==14) {
                e.captcha_img = error.optString("captcha_img");
                e.captcha_sid = error.optString("captcha_sid");
            }
            throw e;
        }
        if(!root.isNull("execute_errors")){
            JSONArray errors=root.getJSONArray("execute_errors");
            if(errors.length()==0)
                return;
            //only first error is processed if there are multiple
            JSONObject error=errors.getJSONObject(0);
            int code=error.getInt("error_code");
            String message=error.getString("error_msg");
            KException e = new KException(code, message, url); 
            if (code==14) {
                e.captcha_img = error.optString("captcha_img");
                e.captcha_sid = error.optString("captcha_sid");
            }
            throw e;
        }
    }
    
    private JSONObject sendRequest(Params params) throws IOException, MalformedURLException, JSONException, KException {
        return sendRequest(params, false);
    }
    
    private final static int MAX_TRIES=3;
    private JSONObject sendRequest(Params params, boolean is_post) throws IOException, MalformedURLException, JSONException, KException {
        String url = getSignedUrl(params, is_post);
        String body="";
        if(is_post)
            body=params.getParamsString();
        Log.i(TAG, "url="+url);
        if(body.length()!=0)
            Log.i(TAG, "body="+body);
        String response="";
        for(int i=1;i<=MAX_TRIES;++i){
            try{
                if(i!=1)
                    Log.i(TAG, "try "+i);
                response = sendRequestInternal(url, body, is_post);
                break;
            }catch(javax.net.ssl.SSLException ex){
                processNetworkException(i, ex);
            }catch(java.net.SocketException ex){
                processNetworkException(i, ex);
            }
        }
        Log.i(TAG, "response="+response);
        JSONObject root=new JSONObject(response);
        checkError(root, url);
        return root;
    }

    private void processNetworkException(int i, IOException ex) throws IOException {
        ex.printStackTrace();
        if(i==MAX_TRIES)
            throw ex;
    }

    private String sendRequestInternal(String url, String body, boolean is_post) throws IOException, MalformedURLException, WrongResponseCodeException {
        HttpURLConnection connection=null;
        try{
            connection = (HttpURLConnection)new URL(url).openConnection();
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(30000);
            connection.setUseCaches(false);
            connection.setDoOutput(is_post);
            connection.setDoInput(true);
            connection.setRequestMethod(is_post?"POST":"GET");
            if(enable_compression)
                connection.setRequestProperty("Accept-Encoding", "gzip");
            if(is_post)
                connection.getOutputStream().write(body.getBytes("UTF-8"));
            int code=connection.getResponseCode();
            Log.i(TAG, "code="+code);
            //It may happen due to keep-alive problem http://stackoverflow.com/questions/1440957/httpurlconnection-getresponsecode-returns-1-on-second-invocation
            if (code==-1)
                throw new WrongResponseCodeException("Network error");
            //может стоит проверить на код 200
            //on error can also read error stream from connection.
            InputStream is = new BufferedInputStream(connection.getInputStream(), 8192);
            String enc=connection.getHeaderField("Content-Encoding");
            if(enc!=null && enc.equalsIgnoreCase("gzip"))
                is = new GZIPInputStream(is);
            String response=Utils.convertStreamToString(is);
            return response;
        }
        finally{
            if(connection!=null)
                connection.disconnect();
        }
    }
    
    private String getSignedUrl(Params params, boolean is_post) {
        params.put("access_token", access_token);
        
        String args = "";
        if(!is_post)
            args=params.getParamsString();
        
        return BASE_URL+params.method_name+"?"+args;
    }
    
    public static String unescape(String text){
        if(text==null)
            return null;
        return text.replace("&amp;", "&").replace("&quot;", "\"").replace("<br>", "\n").replace("&gt;", ">").replace("&lt;", "<")
        .replace("&#39;", "'").replace("<br/>", "\n").replace("&ndash;","-").replace("&#33;", "!").trim();
        //возможно тут могут быть любые коды после &#, например были: 092 - backslash \
    }
    
    public static String unescapeWithSmiles(String text){
        return unescape(text)
                //May be useful to someone
                //.replace("\uD83D\uDE0A", ":-)")
                //.replace("\uD83D\uDE03", ":D")
                //.replace("\uD83D\uDE09", ";-)")
                //.replace("\uD83D\uDE06", "xD")
                //.replace("\uD83D\uDE1C", ";P")
                //.replace("\uD83D\uDE0B", ":p")
                //.replace("\uD83D\uDE0D", "8)")
                //.replace("\uD83D\uDE0E", "B)")
                //
                //.replace("\ud83d\ude12", ":(")  //F0 9F 98 92
                //.replace("\ud83d\ude0f", ":]")  //F0 9F 98 8F
                //.replace("\ud83d\ude14", "3(")  //F0 9F 98 94
                //.replace("\ud83d\ude22", ":'(")  //F0 9F 98 A2
                //.replace("\ud83d\ude2d", ":_(")  //F0 9F 98 AD
                //.replace("\ud83d\ude29", ":((")  //F0 9F 98 A9
                //.replace("\ud83d\ude28", ":o")  //F0 9F 98 A8
                //.replace("\ud83d\ude10", ":|")  //F0 9F 98 90
                //                           
                //.replace("\ud83d\ude0c", "3)")  //F0 9F 98 8C
                //.replace("\ud83d\ude20", ">(")  //F0 9F 98 A0
                //.replace("\ud83d\ude21", ">((")  //F0 9F 98 A1
                //.replace("\ud83d\ude07", "O:)")  //F0 9F 98 87
                //.replace("\ud83d\ude30", ";o")  //F0 9F 98 B0
                //.replace("\ud83d\ude32", "8o")  //F0 9F 98 B2
                //.replace("\ud83d\ude33", "8|")  //F0 9F 98 B3
                //.replace("\ud83d\ude37", ":X")  //F0 9F 98 B7
                //                           
                //.replace("\ud83d\ude1a", ":*")  //F0 9F 98 9A
                //.replace("\ud83d\ude08", "}:)")  //F0 9F 98 88
                //.replace("\u2764", "<3")  //E2 9D A4   
                //.replace("\ud83d\udc4d", ":like:")  //F0 9F 91 8D
                //.replace("\ud83d\udc4e", ":dislike:")  //F0 9F 91 8E
                //.replace("\u261d", ":up:")  //E2 98 9D   
                //.replace("\u270c", ":v:")  //E2 9C 8C   
                //.replace("\ud83d\udc4c", ":ok:")  //F0 9F 91 8C
                ;
    }

    /*** API methods ***/
   
    <T> String arrayToString(Collection<T> items) {
        if(items==null)
            return null;
        String str_cids = "";
        for (Object item:items){
            if(str_cids.length()!=0)
                str_cids+=',';
            str_cids+=item;
        }
        return str_cids;
    }
    /*** methods for photos ***/
    //http://vk.com/dev/photos.getAlbums
    public ArrayList<Album> getAlbums(Long oid, Collection<Long> aids, Integer need_system, Integer need_covers, Integer photo_sizes) throws MalformedURLException, IOException, JSONException, KException{
        Params params = new Params("photos.getAlbums");
        params.put("oid", oid);
        params.put("aids", arrayToString(aids));
        params.put("need_system", need_system);
        params.put("need_covers", need_covers);
        params.put("photo_sizes", photo_sizes);
        JSONObject root = sendRequest(params);
        ArrayList<Album> albums=new ArrayList<Album>();
        JSONArray array=root.optJSONArray("response");
        if (array == null)
            return albums;
        int category_count=array.length(); 
        for (int i=0; i<category_count; ++i) {
            JSONObject o = (JSONObject)array.get(i);
            Album a = Album.parse(o);
            if (a.title.equals("DELETED"))
                continue;
            albums.add(a);
        }
        return albums;
    }
    
    //http://vk.com/dev/photos.get
    public ArrayList<Photo> getPhotos(Long uid, Long aid, Integer offset, Integer count) throws MalformedURLException, IOException, JSONException, KException{
        Params params = new Params("photos.get");
        if(uid>0)
            params.put("uid", uid);
        else
            params.put("gid", -uid);
        params.put("aid", aid);
        params.put("extended", "1");
        params.put("offset",offset);
        params.put("limit",count);
        params.put("v","4.1");
        JSONObject root = sendRequest(params);
        JSONArray array = root.optJSONArray("response");
        if (array == null)
            return new ArrayList<Photo>(); 
        ArrayList<Photo> photos = parsePhotos(array);
        return photos;
    }
    
    //http://vk.com/dev/photos.getUserPhotos
    public ArrayList<Photo> getUserPhotos(Long uid, Integer offset, Integer count) throws MalformedURLException, IOException, JSONException, KException{
        Params params = new Params("photos.getUserPhotos");
        params.put("uid", uid);
        params.put("sort","0");
        params.put("count",count);
        params.put("offset",offset);
        params.put("extended",1);
        JSONObject root = sendRequest(params);
        JSONArray array = root.optJSONArray("response");
        if (array == null)
            return new ArrayList<Photo>(); 
        ArrayList<Photo> photos = parsePhotos(array);
        return photos;
    }

    //http://vk.com/dev/photos.getAll
    public ArrayList<Photo> getAllPhotos(Long owner_id, Integer offset, Integer count, boolean extended) throws MalformedURLException, IOException, JSONException, KException{
        Params params = new Params("photos.getAll");
        params.put("owner_id", owner_id);
        params.put("offset", offset);
        params.put("count",count);
        params.put("extended",extended?1:0);
        JSONObject root = sendRequest(params);
        JSONArray array = root.optJSONArray("response");
        if (array == null)
            return new ArrayList<Photo>(); 
        ArrayList<Photo> photos = parsePhotos(array);
        return photos;
    }
    
    /*** for crate album ***/
    //http://vk.com/dev/photos.createAlbum
    public Album createAlbum(String title, Long gid, String privacy, String comment_privacy, String description) throws MalformedURLException, IOException, JSONException, KException {
        Params params = new Params("photos.createAlbum");
        params.put("title", title);
        params.put("gid", gid);
        params.put("privacy", privacy);
        params.put("comment_privacy", comment_privacy);
        params.put("description", description);
        JSONObject root = sendRequest(params);
        JSONObject o = root.optJSONObject("response");
        if (o == null)
            return null; 
        return Album.parse(o);
    }
    
    //http://vk.com/dev/photos.editAlbum
    public String editAlbum(long aid, Long oid, String title, String privacy, String comment_privacy, String description) throws MalformedURLException, IOException, JSONException, KException {
        Params params = new Params("photos.editAlbum");
        params.put("aid", String.valueOf(aid));
        params.put("oid", oid);
        params.put("title", title);
        params.put("privacy", privacy);
        params.put("comment_privacy", comment_privacy);
        params.put("description", description);
        JSONObject root = sendRequest(params);
        Object response_code = root.opt("response");
        if (response_code != null)
            return String.valueOf(response_code);
        return null;
    }

    //http://vk.com/dev/photos.getUploadServer
    public String photosGetUploadServer(long album_id, Long group_id) throws MalformedURLException, IOException, JSONException, KException {
        Params params = new Params("photos.getUploadServer");
        params.put("aid",album_id);
        params.put("gid",group_id);
        JSONObject root = sendRequest(params);
        JSONObject response = root.getJSONObject("response");
        return response.getString("upload_url");
    }
    
    //http://vk.com/dev/photos.getWallUploadServer
    public String photosGetWallUploadServer(Long user_id, Long group_id) throws MalformedURLException, IOException, JSONException, KException {
        Params params = new Params("photos.getWallUploadServer");
        params.put("uid",user_id);
        params.put("gid",group_id);
        JSONObject root = sendRequest(params);
        JSONObject response = root.getJSONObject("response");
        return response.getString("upload_url");
    }
    
    //http://vk.com/dev/audio.getUploadServer
    public String getAudioUploadServer() throws MalformedURLException, IOException, JSONException, KException {
        Params params = new Params("audio.getUploadServer");
        JSONObject root = sendRequest(params);
        JSONObject response = root.getJSONObject("response");
        return response.getString("upload_url");
    }
    
    //http://vk.com/dev/photos.getMessagesUploadServer
    public String photosGetMessagesUploadServer() throws MalformedURLException, IOException, JSONException, KException {
        Params params = new Params("photos.getMessagesUploadServer");
        JSONObject root = sendRequest(params);
        JSONObject response = root.getJSONObject("response");
        return response.getString("upload_url");
    }
    
    //http://vk.com/dev/photos.getProfileUploadServer
    public String photosGetProfileUploadServer() throws MalformedURLException, IOException, JSONException, KException {
        Params params = new Params("photos.getProfileUploadServer");
        JSONObject root = sendRequest(params);
        JSONObject response = root.getJSONObject("response");
        return response.getString("upload_url");
    }
    
    //http://vk.com/dev/photos.save
    public ArrayList<Photo> photosSave(String server, String photos_list, Long aid, Long group_id, String hash, String caption) throws MalformedURLException, IOException, JSONException, KException {
        Params params = new Params("photos.save");
        params.put("server",server);
        params.put("photos_list",photos_list);
        params.put("aid",aid);
        params.put("gid",group_id);
        params.put("hash",hash);
        params.put("caption",caption);
        JSONObject root = sendRequest(params);
        JSONArray array=root.getJSONArray("response");
        ArrayList<Photo> photos = parsePhotos(array);
        return photos;
    }
    
    //http://vk.com/dev/photos.saveWallPhoto
    public ArrayList<Photo> saveWallPhoto(String server, String photo, String hash, Long user_id, Long group_id) throws MalformedURLException, IOException, JSONException, KException {
        Params params = new Params("photos.saveWallPhoto");
        params.put("server",server);
        params.put("photo",photo);
        params.put("hash",hash);
        params.put("uid",user_id);
        params.put("gid",group_id);
        JSONObject root = sendRequest(params);
        JSONArray array=root.getJSONArray("response");
        ArrayList<Photo> photos = parsePhotos(array);
        return photos;
    }
    //http://vk.com/dev/photos.saveMessagesPhoto
    public ArrayList<Photo> saveMessagesPhoto(String server, String photo, String hash) throws MalformedURLException, IOException, JSONException, KException {
        Params params = new Params("photos.saveMessagesPhoto");
        params.put("server",server);
        params.put("photo",photo);
        params.put("hash",hash);
        JSONObject root = sendRequest(params);
        JSONArray array=root.getJSONArray("response");
        ArrayList<Photo> photos = parsePhotos(array);
        return photos;
    }
    
    //http://vk.com/dev/photos.saveProfilePhoto
    public String[] saveProfilePhoto(String server, String photo, String hash) throws MalformedURLException, IOException, JSONException, KException {
        Params params = new Params("photos.saveProfilePhoto");
        params.put("server",server);
        params.put("photo",photo);
        params.put("hash",hash);
        JSONObject root = sendRequest(params);
        JSONObject response = root.getJSONObject("response");
        String src = response.optString("photo_src");
        String hash1 = response.optString("photo_hash");
        String[] res=new String[]{src, hash1};
        return res;
    }

    private ArrayList<Photo> parsePhotos(JSONArray array) throws JSONException {
        ArrayList<Photo> photos=new ArrayList<Photo>(); 
        int category_count=array.length(); 
        for(int i=0; i<category_count; ++i){
            //in getUserPhotos first element is integer
            if(array.get(i) instanceof JSONObject == false)
                continue;
            JSONObject o = (JSONObject)array.get(i);
            Photo p = Photo.parse(o);
            photos.add(p);
        }
        return photos;
    }

    //http://vk.com/dev/photos.getById
    public ArrayList<Photo> getPhotosById(String photos, Integer extended, Integer photo_sizes) throws MalformedURLException, IOException, JSONException, KException{
        Params params = new Params("photos.getById");
        params.put("photos", photos);
        params.put("extended", extended);
        params.put("photo_sizes", photo_sizes);
        JSONObject root = sendRequest(params);
        JSONArray array = root.optJSONArray("response");
        if (array == null)
            return new ArrayList<Photo>(); 
        ArrayList<Photo> photos1 = parsePhotos(array);
        return photos1;
    }
    
    public Photo getPhotoCountsByIdWithExecute(String photo) throws MalformedURLException, IOException, JSONException, KException {
        String code = "var p=API.photos.getById({\"photos\":\"" + photo + "\",\"extended\":1}); return {\"pid\":p@.pid,\"likes\":p@.likes,\"comments\":p@.comments,\"can_comment\":p@.can_comment,\"tags\":p@.tags};";
        Params params = new Params("execute");
        params.put("code", code);
        JSONObject root = sendRequest(params);
        JSONObject array = root.optJSONObject("response");
        if (array == null)
            return null;
        return Photo.parseCounts(array);
    }    

    //http://vk.com/dev/execute
    public void execute(String code) throws MalformedURLException, IOException, JSONException, KException {
        Params params = new Params("execute");
        params.put("code", code);
        sendRequest(params);
    }
    
    //http://vk.com/dev/photos.delete
    public boolean deletePhoto(Long owner_id, Long photo_id) throws MalformedURLException, IOException, JSONException, KException{
        Params params = new Params("photos.delete");
        params.put("oid", owner_id);
        params.put("pid", photo_id);
        JSONObject root = sendRequest(params);
        long response = root.optLong("response", -1);
        return response==1;
    }

    
    //http://vk.com/dev/photos.deleteAlbum
    public String deleteAlbum(Long aid, Long gid) throws MalformedURLException, IOException, JSONException, KException{
        Params params = new Params("photos.deleteAlbum");
        params.put("aid", aid);
        params.put("gid", gid);
        JSONObject root = sendRequest(params);
        Object response_code = root.opt("response");
        if (response_code != null)
            return String.valueOf(response_code);
        return null;
    }
    
    //http://vk.com/dev/photos.getTags
    public ArrayList<PhotoTag> getPhotoTagsById(Long pid, Long owner_id) throws MalformedURLException, IOException, JSONException, KException{
        Params params = new Params("photos.getTags");
        params.put("owner_id", owner_id);
        params.put("pid", pid);
        JSONObject root = sendRequest(params);
        JSONArray array = root.optJSONArray("response");
        if (array == null)
            return new ArrayList<PhotoTag>(); 
        ArrayList<PhotoTag> photo_tags = parsePhotoTags(array, pid, owner_id);
        return photo_tags;
    }
    
    private ArrayList<PhotoTag> parsePhotoTags(JSONArray array, Long pid, Long owner_id) throws JSONException {
        ArrayList<PhotoTag> photo_tags=new ArrayList<PhotoTag>(); 
        int category_count=array.length(); 
        for(int i=0; i<category_count; ++i){
            //in getUserPhotos first element is integer
            if(array.get(i) instanceof JSONObject == false)
                continue;
            JSONObject o = (JSONObject)array.get(i);
            PhotoTag p = PhotoTag.parse(o);
            photo_tags.add(p);
            if (pid != null)
                p.pid = pid;
            if (owner_id != null)
                p.owner_id = owner_id;
        }
        return photo_tags;
    }

    /*** faves ***/
    
    //http://vk.com/dev/fave.getPhotos
    public ArrayList<Photo> getFavePhotos(Integer count, Integer offset) throws MalformedURLException, IOException, JSONException, KException {
        Params params = new Params("fave.getPhotos");
        params.put("count", count);
        params.put("offset", offset);
        JSONObject root = sendRequest(params);
        JSONArray array = root.optJSONArray("response");
        if (array == null)
            return new ArrayList<Photo>(); 
        ArrayList<Photo> photos = parsePhotos(array);
        return photos;
    }

    /*** end faves  ***/
}