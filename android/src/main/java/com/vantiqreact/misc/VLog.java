package com.vantiqreact.misc;

//import android.content.Context;
//import android.content.Intent;
//import android.content.pm.PackageInfo;
//import android.content.pm.PackageManager;
//import android.os.Build;
import android.util.Log;
//import androidx.core.content.FileProvider;
//import com.vantiqreact.VantiqReactModule;
import org.joda.time.DateTime;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.format.DateTimeFormatter;

/*
    This is a wrapper around the system Log so we can capture the messages and display them elsewhere in the future.
 */
public class VLog
{
    private int currentLogFile;
    private File logFile1;
    private File logFile2;
    private FileWriter currentFileWriter;
    private DateTimeFormatter fmt;

    public long MAXLENGTH = 100000;

    static public VLog INSTANCE;

    public VLog()
    {
        super();

        INSTANCE = this;

        this.initLogs();
    }

    private void concat(File src1, File src2, File dst) throws IOException
    {
        InputStream in1 = new FileInputStream(src1);
        InputStream in2 = new FileInputStream(src2);

        try
        {
            OutputStream out = new FileOutputStream(dst);
            try
            {
                // Transfer bytes from in to out
                byte[] buf = new byte[8096];
                int len;

                if (src1.exists())
                {
                    while ((len = in1.read(buf)) > 0)
                    {
                        out.write(buf, 0, len);
                    }
                }

                if (src2.exists())
                {
                    while ((len = in2.read(buf)) > 0)
                    {
                        out.write(buf, 0, len);
                    }
                }
            }
            finally
            {
                out.close();
            }
        }
        finally
        {
            in1.close();
            in2.close();
        }
    }

    public void send()
    {
        /*
        if (this.currentFileWriter != null)
        {
            String txt1 = "Unknown";

            Context ctx = VantiqReactModule.INSTANCE.application;
            String appName = "???"; //ctx.getString(R.string.app_name);
            try
            {
                PackageInfo pinfo = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0);
                txt1 = String.format(ctx.getString(R.string.version_string), appName, pinfo.versionName, pinfo.versionCode, (BuildConfig.DEBUG ? "*DEBUG*" : ""));
            }
            catch (PackageManager.NameNotFoundException e)
            {
                e.printStackTrace();
            }

            String subject = "Android Logs from " + VantiqReactModule.INSTANCE.currentAccount.HRusername;

            String manufacturer = Build.MANUFACTURER;
            String model = Build.MODEL;
            int version = Build.VERSION.SDK_INT;
            String versionRelease = Build.VERSION.RELEASE;

            String body =
                    "Username: " + VantiqReactModule.INSTANCE.currentAccount.HRusername + "\n" +
                            "Userid: " + VantiqReactModule.INSTANCE.currentAccount.username + "\n" +
                            "Namespace: " + VantiqReactModule.INSTANCE.currentAccount.currentNamespace + "\n" +
                            "Server: " + VantiqReactModule.INSTANCE.currentAccount.server + "\n" +
                            "App: " + txt1 + "\n" +
                            "Device: " + manufacturer + " Model " + model + " Release " + versionRelease + " Version " + version;

            Intent emailIntent = new Intent(Intent.ACTION_SEND);

            emailIntent.setType("text/plain");
            emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"slangley@vantiq.com"});
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
            emailIntent.putExtra(Intent.EXTRA_TEXT, body);

            try
            {
                File logsDir = new File(ctx.getFilesDir(), VantiqReactModule.FS_LOGS);

                File logCopyFile = new File(logsDir, VantiqReactModule.logCopyFileName);

                if (this.currentLogFile == 1)
                {
                    this.currentFileWriter.write("Log File 1 Sending..." + "\n");
                    this.currentFileWriter.flush();
                    this.currentFileWriter.close();

                    this.concat(this.logFile2,this.logFile1,logCopyFile);
                }
                else
                {
                    this.currentFileWriter.write("Log File 2 Sending..." + "\n");
                    this.currentFileWriter.flush();
                    this.currentFileWriter.close();

                    this.concat(this.logFile1,this.logFile2,logCopyFile);
                }

                emailIntent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(ctx, VantiqMode.getFileProvider(), logCopyFile));

                Intent chooserIntent = Intent.createChooser(emailIntent, "Pick an Email provider");
                chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ctx.startActivity(chooserIntent);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }

            DateTime now = new DateTime();

            try
            {
                if (this.currentLogFile == 1)
                {
                    this.currentLogFile = 2;

                    this.currentFileWriter = new FileWriter(this.logFile2);
                    this.currentFileWriter.write("Log File 2 Opened after Send " + fmt.print(now) + "\n");
                }
                else
                {
                    this.currentLogFile = 1;

                    this.currentFileWriter = new FileWriter(this.logFile1);
                    this.currentFileWriter.write("Log File 1 Opened after Send " + fmt.print(now) + "\n");

                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        */

    }

    public void initLogs()
    {
        /*
        this.fmt = ISODateTimeFormat.dateTime();

        Context ctx = VantiqReactModule.INSTANCE.getApplicationContext();

        File logsDir = new File(ctx.getFilesDir(), VantiqReactModule.FS_LOGS);

        if (!logsDir.exists())
        {
            logsDir.mkdirs();
        }

        this.currentLogFile = 0;

        this.logFile1 = new File(logsDir, VantiqReactModule.log1FileName);
        long logFile1LastModified = 0;

        this.logFile2 = new File(logsDir, VantiqReactModule.log2FileName);
        long logFile2LastModified = 0;

        DateTime now = new DateTime();
        DateTimeFormatter fmt = ISODateTimeFormat.dateTime();

        if (this.logFile1.exists())
        {
            logFile1LastModified = this.logFile1.lastModified();
        }
        else
        {
            try
            {
                this.logFile1.createNewFile();
            }
            catch (IOException ex)
            {

            }
        }

        if (logFile2.exists())
        {
            logFile2LastModified = logFile2.lastModified();
        }
        else
        {
            try
            {
                this.logFile2.createNewFile();
            }
            catch (IOException ex)
            {

            }
        }

        if ((logFile1LastModified == 0) && (logFile2LastModified == 0))
        {
            this.currentLogFile = 1;
        }
        else if (logFile1LastModified > logFile2LastModified)
        {
            this.currentLogFile = 2;
        }
        else
        {
            this.currentLogFile = 1;
        }

        if (this.currentLogFile == 1)
        {
            try
            {
                this.currentFileWriter = new FileWriter(this.logFile1);
                this.currentFileWriter.write("Log File 1 Created " + fmt.print(now) + "\n");
            }
            catch (IOException ex)
            {
                ex.printStackTrace();
            }
        }
        else
        {
            try
            {
                this.currentFileWriter =  new FileWriter(this.logFile2);
                this.currentFileWriter.write("Log File 2 Created " + fmt.print(now) + "\n");
            }
            catch (IOException ex)
            {
                ex.printStackTrace();
            }
        }
        */

    }


    public void writeLog(String tag, String lvl, String msg)
    {
        DateTime now = new DateTime();

        if ((this.currentFileWriter != null) && (this.fmt != null))
        {
            long currentSize = 0;

            try
            {
                if (this.currentLogFile == 1)
                {
                    currentSize = this.logFile1.length();

                    if (currentSize > MAXLENGTH)
                    {
                        this.currentLogFile = 2;

                        //this.currentFileWriter.write("Log File 1 Closing " + fmt.print(now) + "\n");

                        this.currentFileWriter.flush();
                        this.currentFileWriter.close();

                        this.currentFileWriter = new FileWriter(this.logFile2);
                        //this.currentFileWriter.write("Log File 2 Created " + fmt.print(now) + "\n");
                    }
                }
                else
                {
                    currentSize = this.logFile2.length();

                    if (currentSize > MAXLENGTH)
                    {
                        this.currentLogFile = 1;

                        //this.currentFileWriter.write("Log File 2 Closing " + fmt.print(now) + "\n");

                        this.currentFileWriter.flush();
                        this.currentFileWriter.close();

                        this.currentFileWriter = new FileWriter(this.logFile1);
                        //this.currentFileWriter.write("Log File 1 Created " + fmt.print(now) + "\n");
                    }
                }

                //this.currentFileWriter.write(this.fmt.print(now) + " " + lvl + " " + tag + " " + msg + "\n");
            }
            catch (Exception e)
            {
                e.printStackTrace();
                this.currentFileWriter = null;
            }
        }
    }

    public static int e(String tag, String msg)
    {
        int rc = Log.e("VantiqInterfaceLibrary",tag + ": " + msg);

        INSTANCE.writeLog(tag,"ERR",msg);
        return rc;

        /*
        DateTime dt = new DateTime();
        DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
        String nowAsString = fmt.print(dt);
        int rc = Log.e(tag,nowAsString + ": " + msg);

        INSTANCE.writeLog(tag,"ERR",msg);
        return rc;
        */
    }
    public static int d(String tag, String msg)
    {
        int rc = Log.d("VantiqInterfaceLibrary",tag + ": " + msg);
        INSTANCE.writeLog(tag,"DBG",msg);
        return rc;
    }
    public static int w(String tag, String msg)
    {
        int rc = Log.w("VantiqInterfaceLibrary",tag + ": " + msg);
        INSTANCE.writeLog(tag,"WRN",msg);
        return rc;
    }
    public static int i(String tag, String msg)
    {
        int rc = Log.i("VantiqInterfaceLibrary",tag + ": " + msg);
        INSTANCE.writeLog(tag,"INF",msg);
        return rc;
    }
    public static int v(String tag, String msg)
    {
        int rc = Log.v("VantiqInterfaceLibrary",tag + ": " + msg);
        INSTANCE.writeLog(tag,"VRB",msg);
        return rc;
    }

    public static int e(String tag, String msg, Throwable tr)
    {
        int rc = Log.e(tag,msg,tr);

        INSTANCE.writeLog(tag,"ERR",msg);

        if (tr != null)
        {
            INSTANCE.writeLog(tag,"EXC",tr.getMessage());

            StackTraceElement[] st = tr.getStackTrace();

            if (st != null)
            {
                for (int i = 0; i < st.length; i++)
                {
                    StackTraceElement ste = st[i];
                    INSTANCE.writeLog(tag, "STK", ste.toString());
                }
            }
        }

        return rc;
    }

    public static int d(String tag, String msg, Throwable tr)
    {
        int rc = Log.d(tag,msg,tr);

        INSTANCE.writeLog(tag,"DBG",msg);

        if (tr != null)
        {
            INSTANCE.writeLog(tag,"EXC",tr.getMessage());

            StackTraceElement[] st = tr.getStackTrace();

            if (st != null)
            {
                for (int i = 0; i < st.length; i++)
                {
                    StackTraceElement ste = st[i];
                    INSTANCE.writeLog(tag, "STK", ste.toString());
                }
            }
        }

        return rc;
    }


}
