#!/usr/bin/env pwsh
#requires -Version 7.0
<#
.SYNOPSIS
    Local Play-readiness gate for Midad Studio.

.DESCRIPTION
    Verifies the relesasable artifacts against the requirements that can be checked
    offline: AAB signature, version/target SDK, permission set, release non-debuggable
    flag and 16 KB ELF alignment. Everything that needs a device, the Play Console or a
    human reviewer is printed at the end as OWNER STEP instead of being claimed as done.

    Run after: gradlew assembleRelease bundleRelease
#>

$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$expectedVersionCode = '5'
$expectedVersionName = '0.2.3'
$buildTools = 'C:\Android\Sdk\build-tools\36.0.0'
$aapt2 = Join-Path $buildTools 'aapt2.exe'
$aab = Join-Path $root 'app\build\outputs\bundle\release\app-release.aab'
$apk = Join-Path $root 'app\build\outputs\apk\release\app-release.apk'
$python = 'python'

$failures = [System.Collections.Generic.List[string]]::new()

function Write-Section([string] $title) {
    Write-Host ''
    Write-Host "== $title" -ForegroundColor Cyan
}

function Write-Pass([string] $message) {
    Write-Host "  [ OK ] $message" -ForegroundColor Green
}

function Write-Fail([string] $message) {
    $script:failures.Add($message)
    Write-Host "  [FAIL] $message" -ForegroundColor Red
}

Write-Section 'Artifacts'
if (-not (Test-Path -LiteralPath $aab)) {
    Write-Fail "missing AAB: $aab (run: gradlew bundleRelease)"
} else {
    $sizeMb = [math]::Round((Get-Item -LiteralPath $aab).Length / 1MB, 1)
    Write-Pass ("AAB exists: {0} ({1} MB)" -f $aab, $sizeMb)
}
$apkExists = Test-Path -LiteralPath $apk
if ($apkExists) {
    $apkMb = [math]::Round((Get-Item -LiteralPath $apk).Length / 1MB, 1)
    Write-Pass ("release APK exists: {0} ({1} MB)" -f $apk, $apkMb)
} else {
    Write-Host '  [skip] no release APK yet; run gradlew assembleRelease for the badging checks' -ForegroundColor Yellow
}

if (Test-Path -LiteralPath $aab) {
    Write-Section 'Signature (AAB)'
    $jarsigner = (Get-Command jarsigner -ErrorAction SilentlyContinue)
    if (-not $jarsigner) {
        Write-Fail 'jarsigner not found on PATH; cannot verify the AAB signature'
    } else {
        $verify = & jarsigner -verify -strict $aab 2>&1 | Out-String
        if ($verify -match 'jar verified') {
            Write-Pass 'AAB is signed and every entry verifies'
            if ($verify -match 'certificate chain') { Write-Host '  [info] self-signed upload certificate is expected for a new app' }
        } else {
            Write-Fail 'AAB signature did not verify (is keystore.properties present?)'
        }
    }

    Write-Section '16 KB page-size compatibility'
    $checker = Join-Path $root 'tools\check_16kb.py'
    if (-not (Test-Path -LiteralPath $checker)) {
        Write-Fail "missing $checker"
    } else {
        $elf = & $python $checker $aab 2>&1 | Out-String
        Write-Host $elf
        if ($LASTEXITCODE -ne 0) { Write-Fail 'one or more native libraries are not 16 KB aligned' }
    }
}

if ($apkExists) {
    Write-Section 'Manifest and permissions (release APK)'
    if (-not (Test-Path -LiteralPath $aapt2)) {
        Write-Fail "missing aapt2 at $aapt2"
    } else {
        $badging = & $aapt2 dump badging $apk 2>&1 | Out-String
        $packageLine = ($badging -split "`n" | Where-Object { $_ -like 'package:*' } | Select-Object -First 1)
        if ($packageLine) {
            if ($packageLine -match "versionCode='(\d+)'") {
                if ($Matches[1] -eq $expectedVersionCode) { Write-Pass "versionCode=$($Matches[1])" } else { Write-Fail "unexpected versionCode $($Matches[1])" }
            }
            if ($packageLine -match "versionName='([^']+)'") {
                if ($Matches[1] -eq $expectedVersionName) { Write-Pass "versionName=$($Matches[1])" } else { Write-Fail "unexpected versionName $($Matches[1])" }
            }
            if ($packageLine -notmatch "packageName='com\.midad\.studio'") { }
        } else {
            Write-Fail 'aapt2 returned no package line'
        }
        if ($badging -match "(?m)^targetSdkVersion:'(\d+)'") {
            if ($Matches[1] -eq '36') { Write-Pass 'targetSdkVersion 36 (Play requirement for new apps since 2026-08-31)' }
            else { Write-Fail "targetSdkVersion is $($Matches[1]), expected 36" }
        } else { Write-Fail 'no targetSdkVersion in the release manifest' }
        if ($badging -match "(?m)^minSdkVersion:'(\d+)'") {
            if ($Matches[1] -eq '26') { Write-Pass 'minSdkVersion 26' }
            else { Write-Fail "unexpected minSdkVersion $($Matches[1])" }
        } else { Write-Fail 'no minSdkVersion in the release manifest' }
        $requested = @(
            [regex]::Matches($badging, "(?m)^uses-permission: name='([^']+)'") |
                ForEach-Object { $_.Groups[1].Value } |
                Where-Object { -not $_.StartsWith('com.midad.studio.') }
        )
        if ($requested.Count -eq 1 -and $requested[0] -eq 'android.permission.INTERNET') {
            Write-Pass 'the only requested permission is android.permission.INTERNET (the com.midad.studio.* ones are app-private signature permissions)'
        } else {
            Write-Fail ("unexpected permission set: " + ($requested -join ', '))
        }
        $localeLine = ($badging -split "`n" | Where-Object { $_ -like 'locales:*' } | Select-Object -First 1)
        $locales = @([regex]::Matches([string]$localeLine, "'([^']+)'") | ForEach-Object { $_.Groups[1].Value })
        if ($locales -contains 'ar') {
            $unexpected = @($locales | Where-Object { $_ -notin @('--_--', 'ar', 'en') })
            if ($unexpected.Count -eq 0) {
                Write-Pass ("shipped locales: " + ($locales -join ', '))
            } else {
                Write-Fail ("unnecessary locales shipped (resourceConfigurations should prevent this): " + ($unexpected -join ', '))
            }
        } else {
            Write-Fail "the release APK does not ship Arabic resources: $localeLine"
        }
        if ($badging -match 'application-debuggable') { Write-Fail 'release APK is debuggable' } else { Write-Pass 'release APK is not debuggable' }
        $apksigner = Join-Path $buildTools 'apksigner.bat'
        if (Test-Path -LiteralPath $apksigner) {
            $apkVerify = & $apksigner verify --print-certs $apk 2>&1 | Out-String
            if ($LASTEXITCODE -eq 0) {
                $digest = ([regex]::Match($apkVerify, 'SHA-256 digest: ([0-9a-f]+)')).Groups[1].Value
                Write-Pass ("release APK signature verifies (cert SHA-256 " + $digest.Substring(0, [Math]::Min(16, $digest.Length)) + "…)")
            } else {
                Write-Fail 'release APK signature did not verify'
            }
        }
        $abiSection = ($badging -split "`n" | Where-Object { $_ -like 'native-code:*' } | Select-Object -First 1)
        $abis = @([regex]::Matches([string]$abiSection, "'([^']+)'") | ForEach-Object { $_.Groups[1].Value })
        if ($abis.Count -ge 2) {
            Write-Pass ("native ABIs: " + ($abis -join ', '))
        } else {
            Write-Fail ("expected at least two ABIs in the universal release APK, saw: " + ($abis -join ', '))
        }
    }
}

Write-Section 'OWNER STEPS (not checkable from this machine)'
$ownerSteps = @(
    'Play Console account type decision: a personal account needs 12 testers on a closed track for 14 continuous days before production access.',
    'Host docs/privacy/index.html at a public HTTPS URL and put that exact URL in the store listing and in the in-app About screen.',
    'Replace <SUPPORT-EMAIL> and the effective date in docs/PRIVACY_POLICY.md and docs/privacy/index.html.',
    'Fill the Play Data safety and content-rating questionnaires using docs/DATA_SAFETY.md.',
    'Arabic phone set exists as internal evidence (emulator); capture the English 8 and a designed icon/feature graphic before listing.',
    'Trademark clearance for the name "Midad Studio" in the target markets.',
    'Physical-device QA on at least one 16 KB-page device and one low-RAM Android 8.0 device.',
    'Keep keystore/midad-upload.jks and keystore.properties backed up and OUT of version control.'
)
foreach ($step in $ownerSteps) { Write-Host "  [OWNER] $step" -ForegroundColor Yellow }

Write-Host ''
if ($failures.Count -gt 0) {
    Write-Host ("RELEASE GATE: FAILED ({0} problem(s))" -f $failures.Count) -ForegroundColor Red
    exit 1
}
Write-Host 'RELEASE GATE: PASSED (all locally checkable requirements)' -ForegroundColor Green
exit 0
