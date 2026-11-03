# 启用仓库级 Git hooks（pre-commit：前端产物一致性校验）。
# 用法：在仓库根执行 powershell -NoProfile -ExecutionPolicy Bypass -File scripts/install-git-hooks.ps1
$ErrorActionPreference = 'Stop'

$root = (& git rev-parse --show-toplevel).Trim()
& git -C $root config core.hooksPath scripts/hooks
if ($LASTEXITCODE -ne 0) {
    throw 'git config core.hooksPath 失败'
}

Write-Host '已启用 pre-commit：core.hooksPath = scripts/hooks'
Write-Host '关闭：git config --unset core.hooksPath'
Write-Host '说明：web/dist 不存在时 hook 自动跳过，不会阻止后端提交。'
