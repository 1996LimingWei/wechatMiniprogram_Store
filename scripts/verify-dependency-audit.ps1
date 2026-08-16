param(
    [switch]$RunOnlineAudit,
    [int]$TimeoutMinutes = 15
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot

function Invoke-CommandWithTimeout {
    param(
        [string]$Command,
        [string]$WorkingDirectory,
        [int]$TimeoutMinutes
    )

    $stdout = New-TemporaryFile
    $stderr = New-TemporaryFile
    $process = [System.Diagnostics.Process]::new()
    $process.StartInfo.FileName = "cmd.exe"
    $process.StartInfo.ArgumentList.Add("/c")
    $process.StartInfo.ArgumentList.Add($Command)
    $process.StartInfo.WorkingDirectory = $WorkingDirectory
    $process.StartInfo.UseShellExecute = $false
    $process.StartInfo.RedirectStandardOutput = $true
    $process.StartInfo.RedirectStandardError = $true

    $outputWriter = [System.IO.StreamWriter]::new($stdout.FullName, $false, [System.Text.Encoding]::UTF8)
    $errorWriter = [System.IO.StreamWriter]::new($stderr.FullName, $false, [System.Text.Encoding]::UTF8)
    $outputHandler = [System.Diagnostics.DataReceivedEventHandler]{
        param($sender, $eventArgs)
        if ($null -ne $eventArgs.Data) {
            $outputWriter.WriteLine($eventArgs.Data)
        }
    }
    $errorHandler = [System.Diagnostics.DataReceivedEventHandler]{
        param($sender, $eventArgs)
        if ($null -ne $eventArgs.Data) {
            $errorWriter.WriteLine($eventArgs.Data)
        }
    }

    try {
        $process.add_OutputDataReceived($outputHandler)
        $process.add_ErrorDataReceived($errorHandler)
        [void]$process.Start()
        $process.BeginOutputReadLine()
        $process.BeginErrorReadLine()
        $completed = $process.WaitForExit($TimeoutMinutes * 60 * 1000)
        if (-not $completed) {
            try {
                $process.Kill($true)
            } catch {
                $process.Kill()
            }
            throw "依赖漏洞扫描命令超时：$Command；超时时间=${TimeoutMinutes}分钟"
        }
        $process.WaitForExit()
    } finally {
        $process.remove_OutputDataReceived($outputHandler)
        $process.remove_ErrorDataReceived($errorHandler)
        $outputWriter.Dispose()
        $errorWriter.Dispose()
        Get-Content -LiteralPath $stdout.FullName -ErrorAction SilentlyContinue
        Get-Content -LiteralPath $stderr.FullName -ErrorAction SilentlyContinue
        Remove-Item -LiteralPath $stdout.FullName, $stderr.FullName -Force -ErrorAction SilentlyContinue
    }

    if ($process.ExitCode -ne 0) {
        throw "依赖漏洞扫描命令失败：$Command"
    }
}

if (-not $RunOnlineAudit) {
    Write-Host "依赖漏洞扫描为重门禁：交付前请执行 scripts/verify-dependency-audit.ps1 -RunOnlineAudit。"
    exit 0
}

Invoke-CommandWithTimeout "corepack pnpm audit --audit-level high" (Join-Path $root "shop-admin") $TimeoutMinutes
Invoke-CommandWithTimeout "mvn -B -DskipTests org.owasp:dependency-check-maven:12.1.8:check ""-DfailBuildOnCVSS=7""" (Join-Path $root "shop-backend") $TimeoutMinutes

Write-Host "依赖漏洞扫描通过。"
