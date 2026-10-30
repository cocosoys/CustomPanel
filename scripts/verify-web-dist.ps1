<#
.SYNOPSIS
    校验前端构建产物（web/dist）与随插件打包的资源（src/main/resources/dist）是否完全一致。

.DESCRIPTION
    按相对路径逐文件比对 SHA-256。存在缺失、多余或内容不同的文件时输出明细并返回 1。
    退出码：0 一致；1 存在差异；2 源目录不存在；3 目标目录不存在。

.PARAMETER Source
    源目录，默认为仓库下的 web/dist。

.PARAMETER Target
    目标目录，默认为仓库下的 src/main/resources/dist。

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File scripts\verify-web-dist.ps1

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File scripts\verify-web-dist.ps1 -Source .\MCERP-Vue2-example\dist
#>
[CmdletBinding()]
param(
    [string]$Source,
    [string]$Target
)

$ErrorActionPreference = 'Stop'

# 参数默认值不能使用 $PSScriptRoot（Windows PowerShell 5.1 在绑定参数时该变量尚未赋值）
if ([string]::IsNullOrWhiteSpace($Source)) {
    $Source = Join-Path $PSScriptRoot '..\web\dist'
}
if ([string]::IsNullOrWhiteSpace($Target)) {
    $Target = Join-Path $PSScriptRoot '..\src\main\resources\dist'
}

function Get-RelativeFileMap {
    param([string]$Root)

    $full = (Resolve-Path -LiteralPath $Root).Path.TrimEnd('\')
    $map = @{}
    foreach ($file in Get-ChildItem -LiteralPath $full -Recurse -File -Force) {
        $relative = $file.FullName.Substring($full.Length).TrimStart([char]'\').Replace('\', '/')
        $map[$relative.ToLowerInvariant()] = [PSCustomObject]@{
            Relative = $relative
            FullName = $file.FullName
            Hash     = (Get-FileHash -LiteralPath $file.FullName -Algorithm SHA256).Hash
        }
    }
    return $map
}

if (-not (Test-Path -LiteralPath $Source -PathType Container)) {
    Write-Host "[错误] 源目录不存在：$Source" -ForegroundColor Red
    Write-Host "       请先进入 web/ 执行 npm run build:prod" -ForegroundColor Yellow
    exit 2
}

if (-not (Test-Path -LiteralPath $Target -PathType Container)) {
    Write-Host "[错误] 目标目录不存在：$Target" -ForegroundColor Red
    exit 3
}

$sourceMap = Get-RelativeFileMap -Root $Source
$targetMap = Get-RelativeFileMap -Root $Target

$missing = @($sourceMap.Keys | Where-Object { -not $targetMap.ContainsKey($_) } | Sort-Object)
$extra = @($targetMap.Keys | Where-Object { -not $sourceMap.ContainsKey($_) } | Sort-Object)
$changed = @(
    $sourceMap.Keys | Where-Object {
        $targetMap.ContainsKey($_) -and $sourceMap[$_].Hash -ne $targetMap[$_].Hash
    } | Sort-Object
)

Write-Host ("源目录：{0}（{1} 个文件）" -f (Resolve-Path -LiteralPath $Source).Path, $sourceMap.Count)
Write-Host ("目标目录：{0}（{1} 个文件）" -f (Resolve-Path -LiteralPath $Target).Path, $targetMap.Count)

if ($missing.Count -eq 0 -and $extra.Count -eq 0 -and $changed.Count -eq 0) {
    Write-Host "[通过] 两目录内容完全一致。" -ForegroundColor Green
    exit 0
}

$shown = 0
if ($missing.Count -gt 0) {
    Write-Host ("`n目标缺失 {0} 个文件：" -f $missing.Count) -ForegroundColor Red
    $missing | Select-Object -First 20 | ForEach-Object { Write-Host "  - $($sourceMap[$_].Relative)" }
    $shown += [Math]::Min(20, $missing.Count)
}
if ($extra.Count -gt 0) {
    Write-Host ("`n目标多余 {0} 个文件：" -f $extra.Count) -ForegroundColor Red
    $extra | Select-Object -First 20 | ForEach-Object { Write-Host "  - $($targetMap[$_].Relative)" }
    $shown += [Math]::Min(20, $extra.Count)
}
if ($changed.Count -gt 0) {
    Write-Host ("`n内容不同 {0} 个文件：" -f $changed.Count) -ForegroundColor Red
    $changed | Select-Object -First 20 | ForEach-Object { Write-Host "  - $($sourceMap[$_].Relative)" }
    $shown += [Math]::Min(20, $changed.Count)
}

$total = $missing.Count + $extra.Count + $changed.Count
if ($total -gt $shown) {
    Write-Host ("`n……其余 {0} 项差异未逐一列出。" -f ($total - $shown))
}

Write-Host "`n[失败] 前后端产物不一致，请重新构建并同步：" -ForegroundColor Red
Write-Host "       robocopy web\dist src\main\resources\dist /MIR /NJH /NJS /NDL /NC /NS" -ForegroundColor Yellow
exit 1
