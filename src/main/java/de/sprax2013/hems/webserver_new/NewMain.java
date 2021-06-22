package de.sprax2013.hems.webserver_new;

import de.sprax2013.hems.webserver.HttpStatusCode;
import de.sprax2013.hems.webserver.Main;
import de.sprax2013.hems.webserver_new.http.HemsWebServer;
import de.sprax2013.hems.webserver_new.routes.DebugRoute;
import de.sprax2013.hems.webserver_new.routes.FileIndexRoute;
import de.sprax2013.hems.webserver_new.routes.ServingRoute;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.regex.Pattern;

public class NewMain {
    public static void main(String[] args) throws Exception {
        File tmpDir = initWorkingDir();
        System.out.println("Using '" + tmpDir.getAbsolutePath() + "' as working directory");

        HemsWebServer srv = new HemsWebServer(true);

        // TODO: Add support for '/name/[:fieldName[?]]' path syntax

        srv
                .get("/debug", new DebugRoute())
                .get("/home", new FileIndexRoute(new File(System.getProperty("user.dir"))))

                .get("/Hallo", (req, res) -> {
                    res.setBody("Ja, hallo.");

                    return true;
                })

                .get(Pattern.compile("/page[0-9]+", Pattern.CASE_INSENSITIVE), (req, res) -> {
                    res.setStatus(HttpStatusCode.OK)
                            .setBody("You found " + req.getFullPath());

                    return true;
                })

                .post("/form.html", (req, res) -> {
                    res.setStatus(HttpStatusCode.OK)
                            .setBody(new String(req.getBody(), StandardCharsets.UTF_8));

                    return true;
                })
                .get("/", new ServingRoute(new File(tmpDir, "www")));

        srv.listen(8080);
        System.out.println("HTTP-Server running at port " + srv.getPort());
        srv.listenSecure(8081);
        System.out.println("HTTPS-Server running at port " + srv.getSecurePort());
    }

    private static File initWorkingDir() throws IOException {
        File result;

        String sysTmpDir = System.getProperty("java.io.tmpdir");

        if (sysTmpDir != null) {
            result = new File(sysTmpDir, Main.class.getPackageName().replace('.', '_'));
        } else {
            result = Files.createTempDirectory(Main.class.getPackageName().replace('.', '_')).toFile();
        }

        String[] files = new String[] {"/www/index.html", "/www/favicon.ico", "/www/form.html"};

        for (String f : files) {
            File dest = new File(result, f);

//            if (!dest.exists()) {
            Files.createDirectories(dest.toPath().getParent());

            try (InputStream in = Main.class.getResourceAsStream(f);
                 FileOutputStream out = new FileOutputStream(dest)) {
                while (in.available() > 0) {
                    out.write(in.read());
                }
            }
//            }
        }

        return result;
    }
}