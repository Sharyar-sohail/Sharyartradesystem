$WshShell = New-Object -ComObject WScript.Shell
$DesktopPath = [System.Environment]::GetFolderPath('Desktop')
$Shortcut = $WshShell.CreateShortcut("$DesktopPath\Binance Trading Terminal.lnk")
$Shortcut.TargetPath = "$PSScriptRoot\launch.bat"
$Shortcut.WorkingDirectory = "$PSScriptRoot"
$Shortcut.IconLocation = "$PSScriptRoot\assets\icon.ico"
$Shortcut.Description = "Binance Automated Trading & Backtesting Terminal"
$Shortcut.Save()
Write-Host "[OK] Desktop shortcut created at $DesktopPath\Binance Trading Terminal.lnk"
