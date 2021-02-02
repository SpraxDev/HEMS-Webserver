package de.sprax2013.hems.webserver;

import de.sprax2013.hems.webserver.routes.DebugRoute;
import de.sprax2013.hems.webserver.routes.FileIndexRoute;
import de.sprax2013.hems.webserver.routes.ServingRoute;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public class Main {
    public static void main(String[] args) throws IOException {
        File tmpDir = initWorkingDir();
        System.out.println("Using '" + tmpDir.getAbsolutePath() + "' as working directory");

        HttpServer srv = new HttpServer(8080);

        srv.addRequestHandler("/home", new FileIndexRoute(new File(System.getProperty("user.dir"))));
        srv.addRequestHandler("/debug", new DebugRoute());

        srv.addRequestHandler("/", new ServingRoute(new File(tmpDir, "www")));

        System.out.println("Webserver running at port " + srv.getPort());
    }

    private static File initWorkingDir() throws IOException {
        File result;

        String sysTmpDir = System.getProperty("java.io.tmpdir");

        if (sysTmpDir != null) {
            result = new File(sysTmpDir, Main.class.getPackageName().replace('.', '_'));
        } else {
            result = Files.createTempDirectory(Main.class.getPackageName().replace('.', '_')).toFile();
        }

        String[] files = new String[] {"/www/index.html", "/www/favicon.ico"};

        for (String f : files) {
            File dest = new File(result, f);

            if (!dest.exists()) {
                Files.createDirectories(dest.toPath().getParent());

                try (InputStream in = Main.class.getResourceAsStream(f);
                     FileOutputStream out = new FileOutputStream(dest)) {
                    while (in.available() > 0) {
                        out.write(in.read());
                    }
                }
            }
        }

        return result;
    }
}