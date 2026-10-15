package dev.aragonite.powersync.client;
import java.io.IOException;
// A failure the owner reads: a string resource plus its format arguments, resolved in the
// device's language when shown. The exception message stays a stable, language-free key.
final class UserError extends IOException {
 final int message;final Object[] args;
 UserError(int message,Object... args){super("powersync-error-"+message);this.message=message;this.args=args;}
 String text(android.content.Context c){return c.getString(message,args);}
}
