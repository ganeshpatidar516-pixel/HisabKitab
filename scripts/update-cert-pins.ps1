# Prints OkHttp-style sha256/ pins for cert_pinning.xml (HisabKitab production host).
param(
    [string]$HostName = "hisabkitab-production-ceea.up.railway.app",
    [string]$Path = "/api/v1/health"
)

$uri = "https://${HostName}${Path}"
$req = [System.Net.HttpWebRequest]::Create($uri)
$req.Timeout = 20000
try {
    $r = $req.GetResponse()
    $r.Close()
} catch {
    if ($_.Exception.Response) { $_.Exception.Response.Close() }
}
$cert = $req.ServicePoint.Certificate
if (-not $cert) {
    Write-Error "No certificate received from $HostName"
    exit 1
}
$x509 = New-Object System.Security.Cryptography.X509Certificates.X509Certificate2 $cert
$spki = $x509.PublicKey.EncodedKeyValue.RawData
$hash = [System.Security.Cryptography.SHA256]::Create().ComputeHash($spki)
$pin = "sha256/" + [Convert]::ToBase64String($hash)
Write-Host "Host: $HostName"
Write-Host "Leaf pin: $pin"
Write-Host "Backup (ISRG Root X1): sha256/C5+lpZ7tcVwmwQIMcRtP6Qt2MRdAuNm+XJlqdwVH3s8="
