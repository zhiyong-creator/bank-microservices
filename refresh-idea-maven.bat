@echo off
echo ========================================
echo  刷新 IDEA Maven 配置
echo ========================================
echo.

echo [1/3] 清理 IDEA 缓存...
if exist ".idea" (
    echo 删除 .idea 目录...
    rd /s /q ".idea"
    echo ✓ 已删除 .idea 目录
) else (
    echo ✓ .idea 目录不存在
)
echo.

echo [2/3] 删除目标文件夹...
for /d %%d in ("accounts\target" "cards\target" "loans\target" "eurekaserver\target" "mybank-bom\target" "mybank-bom\common\target") do (
    if exist "%%d" (
        echo 删除 %%d ...
        rd /s /q "%%d"
    )
)
echo ✓ 已清理所有 target 目录
echo.

echo [3/3] 重新导入 Maven 项目...
echo 请执行以下步骤：
echo 1. 打开 IntelliJ IDEA
echo 2. 选择 File ^> Open
echo 3. 选择目录: D:\code\four\section2_1
echo 4. 等待 IDEA 自动识别 Maven 项目
echo 5. 点击右下角弹出的 "Load Maven Project" 或手动刷新
echo.
echo ========================================
echo  完成！请打开 IDEA 重新加载项目
echo ========================================
pause
