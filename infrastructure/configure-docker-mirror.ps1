# Docker Registry Mirror Configuration Script
# This script helps configure Docker Desktop to use registry mirrors

Write-Host "==================================" -ForegroundColor Cyan
Write-Host "Docker Registry Mirror Setup" -ForegroundColor Cyan
Write-Host "==================================" -ForegroundColor Cyan
Write-Host ""

# Check if Docker is running
Write-Host "Checking Docker status..." -ForegroundColor Yellow
try {
    docker version | Out-Null
    Write-Host "[✓] Docker is running" -ForegroundColor Green
} catch {
    Write-Host "[✗] Docker is not running. Please start Docker Desktop first." -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Current Docker configuration:" -ForegroundColor Yellow
Write-Host "------------------------------------"
docker info | Select-String "Registry"

Write-Host ""
Write-Host "==================================" -ForegroundColor Cyan
Write-Host "Configuration Options:" -ForegroundColor Cyan
Write-Host "==================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Option 1: Configure Docker Desktop Manually" -ForegroundColor Yellow
Write-Host "  1. Right-click Docker Desktop tray icon -> Settings"
Write-Host "  2. Go to 'Docker Engine' section"
Write-Host "  3. Add the following to the JSON config:"
Write-Host ""
Write-Host @"
{
  "builder": {
    "gc": {
      "defaultKeepStorage": "20GB",
      "enabled": true
    }
  },
  "experimental": false,
  "registry-mirrors": [
    "https://registry.cn-hangzhou.aliyuncs.com",
    "https://mirror.ccs.tencentyun.com",
    "https://dockerproxy.com",
    "https://hub-mirror.c.163.com"
  ],
  "insecure-registries": [],
  "debug": false
}
"@ -ForegroundColor White

Write-Host ""
Write-Host "  4. Click 'Apply & Restart'" -ForegroundColor Yellow
Write-Host ""

Write-Host "==================================" -ForegroundColor Cyan
Write-Host "Option 2: Use Mirror Images in docker-compose.yml" -ForegroundColor Yellow
Write-Host "==================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Edit docker-compose.yml and uncomment mirror image lines:" -ForegroundColor White
Write-Host "  For MySQL:  registry.cn-hangzhou.aliyuncs.com/library/mysql:8.0" -ForegroundColor Green
Write-Host "  For MongoDB: registry.cn-hangzhou.aliyuncs.com/library/mongo:7.0" -ForegroundColor Green
Write-Host "  For Redis:  registry.cn-hangzhou.aliyuncs.com/library/redis:7.2-alpine" -ForegroundColor Green
Write-Host ""

Write-Host "==================================" -ForegroundColor Cyan
Write-Host "Option 3: Test Connectivity" -ForegroundColor Yellow
Write-Host "==================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Testing Docker Hub connectivity..." -ForegroundColor Yellow

$registries = @(
    @{Name="Docker Hub"; Url="registry-1.docker.io"},
    @{Name="Aliyun Mirror"; Url="registry.cn-hangzhou.aliyuncs.com"},
    @{Name="Tencent Mirror"; Url="mirror.ccs.tencentyun.com"},
    @{Name="163 Mirror"; Url="hub-mirror.c.163.com"}
)

foreach ($registry in $registries) {
    Write-Host "Testing $($registry.Name) ($($registry.Url))..." -NoNewline
    try {
        $result = Test-NetConnection -ComputerName $registry.Url -Port 443 -WarningAction SilentlyContinue -ErrorAction Stop
        if ($result.TcpTestSucceeded) {
            Write-Host " [✓] Connected" -ForegroundColor Green
        } else {
            Write-Host " [✗] Failed" -ForegroundColor Red
        }
    } catch {
        Write-Host " [✗] Failed" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "==================================" -ForegroundColor Cyan
Write-Host "Next Steps:" -ForegroundColor Cyan
Write-Host "==================================" -ForegroundColor Cyan
Write-Host "1. Choose one of the configuration options above" -ForegroundColor White
Write-Host "2. After configuring, test by running:" -ForegroundColor White
Write-Host "   docker pull redis:7.2-alpine" -ForegroundColor Green
Write-Host "3. If successful, run:" -ForegroundColor White
Write-Host "   docker-compose up -d" -ForegroundColor Green
Write-Host ""
