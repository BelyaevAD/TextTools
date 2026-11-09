using System;
using System.Diagnostics;
using System.IO;
using System.Text;

// jpackage always executes light.exe. WiX 3 rejects one light call that lists
// several cultures, so this exe forwards that call to light-multilang.ps1.
internal static class LightShim
{
    private static int Main(string[] args)
    {
        string dir = Path.GetDirectoryName(typeof(LightShim).Assembly.Location);
        string script = Path.Combine(dir, "light-multilang.ps1");
        string argFile = Path.Combine(dir, "light-args.txt");
        File.WriteAllLines(argFile, args, new UTF8Encoding(false));
        var command = new StringBuilder();
        command.Append("-NoLogo -NoProfile -ExecutionPolicy Bypass -File ");
        command.Append(Quote(script));
        var start = new ProcessStartInfo("powershell.exe", command.ToString())
        {
            UseShellExecute = false
        };
        Process process = Process.Start(start);
        process.WaitForExit();
        return process.ExitCode;
    }

    private static string Quote(string value)
    {
        if (value.IndexOfAny(new[] { ' ', '\t', '"' }) < 0)
        {
            return value;
        }
        return "\"" + value.Replace("\"", "\\\"") + "\"";
    }
}
