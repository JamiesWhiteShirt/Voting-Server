package info.modoff.votingserver;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class VoteCodeClient {
    private final URL url;
    private final String secret;
    private final int timeout;
    private final String userAgent;

    public VoteCodeClient(URL url, String secret, int timeout, String userAgent) {
        this.url = url;
        this.secret = secret;
        this.timeout = timeout;
        this.userAgent = userAgent;
    }

    public String getSync(UUID uuid) throws VoteCodeException {
        String urlParameters;
        try {
            urlParameters = "secret=" + URLEncoder.encode(secret, "UTF-8") + "&uuid=" + URLEncoder.encode(uuid.toString(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // This should never happen
            throw new VoteCodeException("Unsupported encoding", e);
        }
        byte[] postData = urlParameters.getBytes(StandardCharsets.UTF_8);

        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(timeout);
            connection.setDoOutput(true);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("User-Agent", userAgent);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("charset", "utf-8");
            connection.setRequestProperty("Content-Length", Integer.toString(postData.length));
            connection.setUseCaches(false);

            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            wr.write(postData);

            try {
                JsonParser parser = new JsonParser();
                JsonObject response = parser.parse(new InputStreamReader(connection.getInputStream())).getAsJsonObject();
                if (response.get("success").getAsBoolean()) {
                    return response.get("code").getAsString();
                } else {
                    JsonElement message = response.get("message");
                    if (message != null) {
                        throw new VoteCodeException("Request was unsuccessful (" + message + ")");
                    } else {
                        throw new VoteCodeException("Request was unsuccessful (no message)");
                    }
                }
            } catch (ClassCastException e) {
                // ClassCastExceptions will be thrown by getAs_ methods if the element is not the right type
                throw new VoteCodeException("Response is malformed", e);
            }
        } catch (IOException e) {
            throw new VoteCodeException("IO error (" + e.getMessage() + ")", e);
        }
    }

    public CompletableFuture<String> getAsync(UUID uuid, Executor executor) {
        CompletableFuture<String> result = new CompletableFuture<>();
        executor.execute(() -> {
            try {
                result.complete(getSync(uuid));
            } catch (VoteCodeException e) {
                result.completeExceptionally(e);
            }
        });
        return result;
    }
}
