package src.engine;

public abstract class Application extends Window {

  protected void preRun() {

  }

  protected void postRun() {

  }

  public static void launch(final Application app) {
    app.initWindow();
    app.preRun();
    app.run();
    app.postRun();
    app.destroy();
  }
}
