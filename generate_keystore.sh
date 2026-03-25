#!/bin/bash
# =============================================================
#  generate_keystore.sh
#  一键生成签名密钥，并输出配置 GitHub Secrets 所需的 Base64
# =============================================================

set -e

echo "=========================================="
echo "  屏幕翻译 APK 签名密钥生成工具"
echo "=========================================="
echo ""

# 默认参数（可自行修改）
KEYSTORE_FILE="screen_translator.jks"
KEY_ALIAS="screen_translator_key"
KEY_STORE_PASSWORD="StrongPassword123"
KEY_PASSWORD="StrongPassword123"
VALIDITY_DAYS=10000

echo "📋 密钥配置："
echo "   文件名      : $KEYSTORE_FILE"
echo "   Key Alias   : $KEY_ALIAS"
echo "   有效期      : $VALIDITY_DAYS 天 (~27 年)"
echo ""

# 生成 keystore
keytool -genkeypair \
  -v \
  -keystore "$KEYSTORE_FILE" \
  -alias "$KEY_ALIAS" \
  -keyalg RSA \
  -keysize 2048 \
  -validity "$VALIDITY_DAYS" \
  -storepass "$KEY_STORE_PASSWORD" \
  -keypass "$KEY_PASSWORD" \
  -dname "CN=ScreenTranslator, OU=Dev, O=Personal, L=Unknown, S=Unknown, C=CN"

echo ""
echo "✅ 密钥生成成功：$KEYSTORE_FILE"
echo ""
echo "=========================================="
echo "  以下是需要添加到 GitHub Secrets 的值"
echo "  (仓库 → Settings → Secrets → Actions)"
echo "=========================================="
echo ""
echo "Secret 名称            │ 值"
echo "───────────────────────┼──────────────────────────────────────"
echo "SIGNING_KEY            │ (见下方 Base64 输出)"
echo "KEY_ALIAS              │ $KEY_ALIAS"
echo "KEY_STORE_PASSWORD     │ $KEY_STORE_PASSWORD"
echo "KEY_PASSWORD           │ $KEY_PASSWORD"
echo ""
echo "─── SIGNING_KEY (Base64) ───────────────────"
base64 -w 0 "$KEYSTORE_FILE"
echo ""
echo "────────────────────────────────────────────"
echo ""
echo "⚠️  请妥善保管 $KEYSTORE_FILE 文件！"
echo "   发布到 Google Play 后，同一 App 必须始终用同一密钥签名。"
