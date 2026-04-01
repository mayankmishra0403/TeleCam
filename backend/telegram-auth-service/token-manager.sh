#!/bin/bash

# 🔐 TeleCam - Token Management & Security Helper Script
# Usage: bash token-manager.sh <command>
# Commands: setup, rotate, check, validate

set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
BACKEND_DIR="$PROJECT_ROOT/backend/telegram-auth-service"
ENV_EXAMPLE="$BACKEND_DIR/.env.example"
ENV_LOCAL="$BACKEND_DIR/.env.local"
ROTATION_LOG="$PROJECT_ROOT/.token-rotation-history.log"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Helper functions
log_info() { echo -e "${BLUE}ℹ${NC} $1"; }
log_success() { echo -e "${GREEN}✓${NC} $1"; }
log_warning() { echo -e "${YELLOW}⚠${NC} $1"; }
log_error() { echo -e "${RED}✗${NC} $1"; }

# 1. Setup - Initialize local env for development
setup() {
    log_info "Setting up local development environment..."
    
    if [ -f "$ENV_LOCAL" ]; then
        log_warning "$ENV_LOCAL already exists. Skipping setup."
        return 0
    fi
    
    # Copy from example
    cp "$ENV_EXAMPLE" "$ENV_LOCAL"
    log_success "Created $ENV_LOCAL"
    
    # Prompt for token
    read -sp "Enter your Telegram BOT_TOKEN from BotFather: " bot_token
    echo
    
    if [ -z "$bot_token" ]; then
        log_error "Bot token cannot be empty"
        return 1
    fi
    
    # Update .env.local with actual token
    sed -i.bak "s/BOT_TOKEN=.*/BOT_TOKEN=$bot_token/" "$ENV_LOCAL"
    rm -f "$ENV_LOCAL.bak"
    
    log_success "Bot token saved to $ENV_LOCAL (local only, not tracked)"
    log_warning "Reminder: $ENV_LOCAL is in .gitignore and will NOT be committed"
    
    # Test connection
    validate_token "$bot_token"
}

# 2. Rotate - Create new token (requires BotFather interaction, logging only)
rotate() {
    log_warning "Token rotation requires BotFather manual action"
    echo ""
    echo "📱 Steps to rotate your bot token:"
    echo "  1. Open Telegram and chat with @BotFather"
    echo "  2. Send: /token"
    echo "  3. Select your bot (@tele_ca_m_bot)"
    echo "  4. Copy the NEW token"
    echo "  5. Run: bash token-manager.sh check"
    echo "  6. Enter the new token"
    echo ""
    
    # Log rotation attempt
    timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    echo "$timestamp - Token rotation initiated" >> "$ROTATION_LOG"
    
    # Prompt for new token
    read -sp "Enter new token from BotFather: " new_token
    echo
    
    if [ -z "$new_token" ]; then
        log_error "Token cannot be empty"
        return 1
    fi
    
    # Update local env
    sed -i.bak "s/BOT_TOKEN=.*/BOT_TOKEN=$new_token/" "$ENV_LOCAL"
    rm -f "$ENV_LOCAL.bak"
    
    log_success "Local token updated in $ENV_LOCAL"
    log_warning "Next: Update Railway dashboard Variables with new token"
    
    # Validate
    validate_token "$new_token"
    
    # Log success
    echo "$timestamp - Token rotation completed successfully" >> "$ROTATION_LOG"
}

# 3. Check - Verify current token validity
check() {
    if [ ! -f "$ENV_LOCAL" ]; then
        log_error ".env.local not found. Run: bash token-manager.sh setup"
        return 1
    fi
    
    # Extract token from .env.local
    current_token=$(grep "BOT_TOKEN=" "$ENV_LOCAL" | cut -d'=' -f2 | tr -d ' ')
    
    if [ -z "$current_token" ] || [ "$current_token" = "YOUR_TELEGRAM_BOT_TOKEN" ]; then
        log_error "No valid token found in $ENV_LOCAL"
        return 1
    fi
    
    validate_token "$current_token"
}

# 4. Validate - Test token against Telegram API
validate_token() {
    local token="$1"
    
    log_info "Validating token with Telegram API..."
    
    response=$(curl -s "https://api.telegram.org/bot${token}/getMe")
    
    if echo "$response" | grep -q '"ok":true'; then
        bot_name=$(echo "$response" | grep -o '"first_name":"[^"]*' | cut -d'"' -f4)
        bot_user=$(echo "$response" | grep -o '"username":"[^"]*' | cut -d'"' -f4)
        log_success "Token is valid ✓"
        echo "  Bot: @$bot_user ($bot_name)"
        echo "  Token expires: Never (permanent until revoked)"
        return 0
    else
        log_error "Token validation failed"
        echo "  Response: $response"
        return 1
    fi
}

# 5. Deploy - Update Railway with new token
deploy_to_railway() {
    log_info "Updating Railway with new token..."
    
    if [ ! -f "$ENV_LOCAL" ]; then
        log_error ".env.local not found"
        return 1
    fi
    
    current_token=$(grep "BOT_TOKEN=" "$ENV_LOCAL" | cut -d'=' -f2 | tr -d ' ')
    
    log_warning "Manual step required:"
    echo "  1. Go to https://railway.app/dashboard"
    echo "  2. Select 'telegram-auth-service' project"
    echo "  3. Click 'Variables'"
    echo "  4. Update BOT_TOKEN: $current_token"
    echo "  5. Save and redeploy"
    echo ""
    read -p "Press Enter after updating Railway..."
    
    log_success "Railway redeploy initiated"
}

# 6. Status - Show current setup status
status() {
    echo ""
    echo "📊 TeleCam Token Management Status"
    echo "=================================="
    echo ""
    
    # Check local setup
    if [ -f "$ENV_LOCAL" ]; then
        log_success ".env.local exists (local development ready)"
    else
        log_warning ".env.local not found (run: bash token-manager.sh setup)"
    fi
    
    # Check .env.example
    if [ -f "$ENV_EXAMPLE" ]; then
        log_success ".env.example exists (tracked, placeholder only)"
    else
        log_error ".env.example missing"
    fi
    
    # Check git tracking
    if git check-ignore "$ENV_LOCAL" >/dev/null 2>&1; then
        log_success "$ENV_LOCAL is in .gitignore (will not be committed)"
    else
        log_error "$ENV_LOCAL NOT in .gitignore (security risk!)"
    fi
    
    # Show rotation history
    if [ -f "$ROTATION_LOG" ]; then
        echo ""
        echo "🔄 Token Rotation History:"
        tail -3 "$ROTATION_LOG" | sed 's/^/  /'
    fi
    
    echo ""
}

# Main
case "${1:-help}" in
    setup)
        setup
        ;;
    rotate)
        rotate
        ;;
    check)
        check
        ;;
    validate)
        if [ -z "${2:-}" ]; then
            log_error "Usage: bash token-manager.sh validate <token>"
            exit 1
        fi
        validate_token "$2"
        ;;
    deploy)
        deploy_to_railway
        ;;
    status)
        status
        ;;
    help|*)
        cat << 'EOF'

🔐 TeleCam Token Manager
========================

Commands:
  setup       → Initialize local .env.local with your bot token
  rotate      → Start token rotation process (manual BotFather steps)
  check       → Validate current token in .env.local
  validate    → Test specific token against Telegram API
  deploy      → Update Railway with new token (manual steps)
  status      → Show token setup & rotation history
  help        → Show this message

Example:
  bash token-manager.sh setup      # First time setup
  bash token-manager.sh check      # Verify token works
  bash token-manager.sh rotate     # Rotate token (every 90 days)
  bash token-manager.sh deploy     # Push to production

Security Notes:
  ✓ .env.local is in .gitignore (never committed)
  ✓ .env.example has placeholders only (safe to track)
  ✓ Railway Secrets are encrypted at rest
  ✓ Never hardcode tokens in source code

EOF
        ;;
esac

