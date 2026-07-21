$Script:DefaultMysqlUrl = 'ENC(B36CvdDaY28JXxYsG8R2I+AnEQ1etkta8vWec0knNxfVF5S3p9LLU6gDlpTFyEXPStXSOQBCJKQmsL/5o6NEBI6u0ek/LihaAvvtabkbsQ7Cr/0TgiQzKCy3IYfgjy1NZjKvgrOBfiewPOpXX7HemFkwGQwjrQVl+P2hHnVXj1bHDnpI60qkXghOVgOO0hK60FChFsHf84+v29M8NzO3tfTAubtNyNta)'
$Script:DefaultMysqlUser = 'ENC(1LHxXPcEkqlKu+CAEHfYwwwHnYs34RLE)'
$Script:DefaultMysqlPassword = 'ENC(XMqk4InL9cw/0if28U3q+pdYQ9k1U1UEn1/i2sDhODs=)'

function Get-ConfigDecryptKey {
    if ($env:JASYPT_ENCRYPTOR_PASSWORD) {
        return $env:JASYPT_ENCRYPTOR_PASSWORD
    }

    $secure = Read-Host "Input config decrypt key" -AsSecureString
    $bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
    try {
        $plain = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
    }
    if ([string]::IsNullOrWhiteSpace($plain)) {
        throw "Config decrypt key is required"
    }
    $env:JASYPT_ENCRYPTOR_PASSWORD = $plain
    return $plain
}

function ConvertFrom-JasyptValue {
    param(
        [Parameter(Mandatory=$true)][string]$Value,
        [Parameter(Mandatory=$true)][string]$Key
    )

    if ($Value -notmatch '^ENC\((.+)\)$') {
        return $Value
    }

    $runDir = Join-Path (Split-Path -Parent $PSScriptRoot) '.run'
    if (-not (Test-Path $runDir)) {
        New-Item -ItemType Directory -Path $runDir -Force | Out-Null
    }
    $source = Join-Path $runDir 'JasyptDecryptor.java'
    if (-not (Test-Path $source)) {
        $javaSource = @(
            'import javax.crypto.Cipher;',
            'import javax.crypto.SecretKey;',
            'import javax.crypto.SecretKeyFactory;',
            'import javax.crypto.spec.PBEKeySpec;',
            'import javax.crypto.spec.PBEParameterSpec;',
            'import java.nio.charset.StandardCharsets;',
            'import java.util.Arrays;',
            'import java.util.Base64;',
            '',
            'public class JasyptDecryptor {',
            '    public static void main(String[] args) throws Exception {',
            '        String password = System.getenv("JASYPT_DECRYPT_KEY");',
            '        byte[] all = Base64.getDecoder().decode(args[0]);',
            '        byte[] salt = Arrays.copyOfRange(all, 0, 8);',
            '        byte[] data = Arrays.copyOfRange(all, 8, all.length);',
            '        SecretKey key = SecretKeyFactory.getInstance("PBEWithMD5AndDES")',
            '                .generateSecret(new PBEKeySpec(password.toCharArray()));',
            '        Cipher cipher = Cipher.getInstance("PBEWithMD5AndDES");',
            '        cipher.init(Cipher.DECRYPT_MODE, key, new PBEParameterSpec(salt, 1000));',
            '        System.out.print(new String(cipher.doFinal(data), StandardCharsets.UTF_8));',
            '    }',
            '}'
        ) -join [Environment]::NewLine
        $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
        [IO.File]::WriteAllText($source, $javaSource, $utf8NoBom)
    }

    $oldKey = $env:JASYPT_DECRYPT_KEY
    $env:JASYPT_DECRYPT_KEY = $Key
    try {
        $inner = $Matches[1]
        $java = $null
        $javac = Get-Command javac -ErrorAction SilentlyContinue
        if ($javac) {
            $candidate = Join-Path (Split-Path $javac.Source) 'java.exe'
            if (Test-Path $candidate) { $java = $candidate }
        }
        if (-not $java) {
            $java = (Get-Command java -ErrorAction Stop).Source
        }
        $plain = & $java $source $inner
        if ($LASTEXITCODE -ne 0) {
            throw "Config decrypt failed"
        }
        return $plain
    } finally {
        if ($null -eq $oldKey) {
            Remove-Item Env:\JASYPT_DECRYPT_KEY -ErrorAction SilentlyContinue
        } else {
            $env:JASYPT_DECRYPT_KEY = $oldKey
        }
    }
}

function Get-MysqlConfig {
    $key = Get-ConfigDecryptKey
    $urlValue = $Script:DefaultMysqlUrl
    $userValue = $Script:DefaultMysqlUser
    $passwordValue = $Script:DefaultMysqlPassword
    if ($env:MYSQL_URL) { $urlValue = $env:MYSQL_URL }
    if ($env:MYSQL_USER) { $userValue = $env:MYSQL_USER }
    if ($env:MYSQL_PASSWORD) { $passwordValue = $env:MYSQL_PASSWORD }
    $url = ConvertFrom-JasyptValue -Value $urlValue -Key $key
    $user = ConvertFrom-JasyptValue -Value $userValue -Key $key
    $password = ConvertFrom-JasyptValue -Value $passwordValue -Key $key

    $jdbcPrefix = 'jdbc' + ':'
    if (-not $url.StartsWith($jdbcPrefix)) {
        throw "MYSQL_URL format is invalid"
    }
    $uri = [Uri]$url.Substring($jdbcPrefix.Length)

    $port = '3306'
    if ($uri.Port -gt 0) { $port = [string]$uri.Port }
    $config = New-Object psobject
    $config | Add-Member -NotePropertyName Host -NotePropertyValue $uri.Host
    $config | Add-Member -NotePropertyName Port -NotePropertyValue $port
    $config | Add-Member -NotePropertyName Database -NotePropertyValue $uri.AbsolutePath.TrimStart('/')
    $config | Add-Member -NotePropertyName User -NotePropertyValue $user
    $config | Add-Member -NotePropertyName Password -NotePropertyValue $password
    return $config
}

function Invoke-MysqlScalar {
    param(
        [Parameter(Mandatory=$true)]$Config,
        [Parameter(Mandatory=$true)][string]$Sql
    )

    $mysql = Get-Command mysql -ErrorAction Stop
    $runDir = Join-Path (Split-Path -Parent $PSScriptRoot) '.run'
    if (-not (Test-Path $runDir)) {
        New-Item -ItemType Directory -Path $runDir -Force | Out-Null
    }
    $optionFile = Join-Path $runDir ("mysql-client-{0}.cnf" -f ([Guid]::NewGuid().ToString('N')))
    $content = @"
[client]
host=$($Config.Host)
port=$($Config.Port)
user=$($Config.User)
password=$($Config.Password)
database=$($Config.Database)
"@
    try {
        $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
        [IO.File]::WriteAllText($optionFile, $content, $utf8NoBom)
        $result = & $mysql.Source "--defaults-extra-file=$optionFile" --batch --skip-column-names --raw -e $Sql 2>&1
        if ($LASTEXITCODE -ne 0) {
            throw ($result | Out-String)
        }
        return (($result | Select-Object -First 1) -as [string]).Trim()
    } finally {
        Remove-Item $optionFile -Force -ErrorAction SilentlyContinue
    }
}

function Test-DatabaseConnection {
    $config = Get-MysqlConfig
    [void](Invoke-MysqlScalar -Config $config -Sql 'SELECT 1')
    Write-Host "[OK] MySQL connection check passed" -ForegroundColor Green
}

function Reset-ProjectDatabase {
    param([switch]$IncludeApproval)

    $config = Get-MysqlConfig
    $tables = @(
        'flyway_rule_engine_schema_history',
        'flyway_approval_schema_history',
        'flyway_schema_history',
        're_rule_test_run',
        're_rule_test_suite_case',
        're_rule_test_suite',
        're_rule_test_case',
        're_custom_field',
        're_idempotency_key',
        're_rule_template',
        're_webhook_log',
        're_webhook_config',
        're_rule_product_binding',
        're_rule_set_step',
        're_rule_set',
        're_rule_audit_log',
        're_rule_execution_log',
        're_rule_version',
        're_rule_definition'
    )
    if ($IncludeApproval) {
        $tables += 're_approval_record'
    }

    $quoted = ($tables | ForEach-Object { "``$_``" }) -join ','
    $sql = "SET FOREIGN_KEY_CHECKS=0; DROP TABLE IF EXISTS $quoted; SET FOREIGN_KEY_CHECKS=1;"
    [void](Invoke-MysqlScalar -Config $config -Sql $sql)
    Write-Host "[OK] MySQL project tables reset; waiting for Flyway init" -ForegroundColor Green
}

function Test-DatabaseState {
    param([switch]$IncludeApproval)

    $config = Get-MysqlConfig
    $tables = @(
        're_rule_definition',
        're_rule_version',
        're_rule_execution_log',
        're_rule_audit_log',
        're_rule_set',
        're_rule_set_step',
        're_rule_product_binding',
        're_webhook_config',
        're_webhook_log',
        're_rule_template',
        're_idempotency_key',
        're_custom_field',
        're_rule_test_case',
        're_rule_test_suite',
        're_rule_test_suite_case',
        're_rule_test_run'
    )
    if ($IncludeApproval) {
        $tables += 're_approval_record'
    }

    $quoted = ($tables | ForEach-Object { "'$_'" }) -join ','
    $sql = 'SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name IN (' + $quoted + ')'
    $count = [int](Invoke-MysqlScalar -Config $config -Sql $sql)
    if ($count -ne $tables.Count) {
        throw "MySQL table check failed: expected $($tables.Count), found $count. Check Flyway startup logs."
    }

    $templateCount = [int](Invoke-MysqlScalar -Config $config -Sql 'SELECT COUNT(*) FROM re_rule_template')
    if ($templateCount -le 0) {
        throw "MySQL seed check failed: re_rule_template is empty"
    }

    Write-Host "[OK] MySQL tables and seed data check passed" -ForegroundColor Green
}
