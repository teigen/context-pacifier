package test;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;

public class Run {

    public static void main(String[] args){
        try {
            run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void run() throws Exception {
        final Server server = new Server(8080);
        WebAppContext context = new WebAppContext();
        context.setContextPath("/demo");
        context.setWar("src/test/webapp");
        server.setHandler(context);
        server.start();
        new Thread() {
            @Override
            public void run() {
                System.console().readLine("ENTER TO STOP:");
                try {
                    server.stop();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
        server.join();
    }
}
