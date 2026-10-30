param([switch]$CheckRestart)
$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path $PSScriptRoot -Parent
$config = @{}
Get-Content -LiteralPath (Join-Path $repoRoot 'infra\.env') | ForEach-Object { if ($_ -match '^([^#=]+)=(.*)$') { $config[$matches[1]]=$matches[2] } }
$base = 'http://127.0.0.1:' + $config['ADMIN_PORT']
$studentBase = 'http://127.0.0.1:' + $config['WEB_PORT']
function Send-DemoJson($path,$body,$method='POST') {
    $bytes=[System.Text.Encoding]::UTF8.GetBytes(($body | ConvertTo-Json))
    return Invoke-RestMethod ($base+$path) -Method $method -ContentType 'application/json; charset=utf-8' -Body $bytes
}
function Expect-Status($path,$body,$method,$expected) {
    $rejected=$false
    try { Send-DemoJson $path $body $method | Out-Null }
    catch { if ([int]$_.Exception.Response.StatusCode -eq $expected) { $rejected=$true } else { throw } }
    if (-not $rejected) { throw ('Expected HTTP '+$expected+' for '+$path) }
}
if ((Invoke-RestMethod ($base+'/api/health')).status -ne 'UP') { throw 'Backend/dependencies not ready' }
$marker='Demo learner '+[DateTime]::UtcNow.ToString('yyyyMMdd-HHmmss-fff')
$student=Send-DemoJson '/api/students' @{name=$marker}
$lesson=Send-DemoJson '/api/lessons' @{studentId=$student.id;topic='Fractions';performance='Explained the diagram clearly';homework='Practise questions 1-3'}
$draft=Invoke-RestMethod ($base+'/api/lessons/'+$lesson.id+'/feedback') -Method Post
$repeat=Invoke-RestMethod ($base+'/api/lessons/'+$lesson.id+'/feedback') -Method Post
if ($draft.status -ne 'DRAFT' -or $repeat.id -ne $draft.id) { throw 'Draft uniqueness failed' }
$unpublished=Invoke-RestMethod ($studentBase+'/api/students/'+$student.id+'/feedback')
if ($unpublished | Where-Object id -eq $draft.id) { throw 'Draft was exposed in student endpoint' }
$content='Teacher confirmed: draw a diagram before writing the equation.'
Send-DemoJson ('/api/feedback/'+$draft.id) @{content=$content} 'PUT' | Out-Null
$confirmed=Invoke-RestMethod ($base+'/api/feedback/'+$draft.id+'/confirm') -Method Post
$again=Invoke-RestMethod ($base+'/api/feedback/'+$draft.id+'/confirm') -Method Post
if ($confirmed.status -ne 'CONFIRMED' -or $again.id -ne $draft.id) { throw 'Confirmation failed' }
$published=Invoke-RestMethod ($studentBase+'/api/students/'+$student.id+'/feedback')
if (-not ($published | Where-Object { $_.id -eq $draft.id -and $_.content -eq $content })) { throw 'Confirmed feedback missing' }
Expect-Status ('/api/feedback/'+$draft.id) @{content='Must not overwrite'} 'PUT' 409
$beforeInvalid=(Invoke-RestMethod ($base+'/api/stats')).students
Expect-Status '/api/students' @{name='  '} 'POST' 400
if ((Invoke-RestMethod ($base+'/api/stats')).students -ne $beforeInvalid) { throw 'Invalid input changed database' }
if ($CheckRestart) {
    Push-Location $repoRoot
    try {
        & docker compose --env-file infra/.env -f infra/compose.yaml down
        if ($LASTEXITCODE -ne 0) { throw 'Stop failed' }
        & docker compose --env-file infra/.env -f infra/compose.yaml up -d
        if ($LASTEXITCODE -ne 0) { throw 'Restart failed' }
        $ready=$false
        for ($attempt=0;$attempt -lt 60;$attempt++) {
            try { if ((Invoke-RestMethod ($base+'/api/health') -TimeoutSec 3).status -eq 'UP') { $ready=$true;break } }
            catch { Start-Sleep -Seconds 2 }
        }
        if (-not $ready) { throw 'Restart readiness failed' }
        $afterRestart=Invoke-RestMethod ($studentBase+'/api/students/'+$student.id+'/feedback')
        if (-not ($afterRestart | Where-Object { $_.id -eq $draft.id -and $_.status -eq 'CONFIRMED' })) { throw 'Persistence after recreation failed' }
    } finally { Pop-Location }
}
$result=@{passed=$true;studentId=$student.id;lessonId=$lesson.id;feedbackId=$draft.id;draftHidden=$true;confirmationIdempotent=$true;confirmedReadOnly=$true;invalidInputRejected=$true;persistenceChecked=[bool]$CheckRestart}
$outputDir=Join-Path $repoRoot 'output'
New-Item -ItemType Directory -Path $outputDir -Force | Out-Null
[System.IO.File]::WriteAllText((Join-Path $outputDir 'verification.json'),($result | ConvertTo-Json),[System.Text.UTF8Encoding]::new($false))
Write-Host ('PASS class feedback flow; feedback id='+$draft.id+'; persistence='+$CheckRestart)
Write-Host 'This verification adds fictional records and keeps existing data.'
