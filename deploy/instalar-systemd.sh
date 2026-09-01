#!/usr/bin/env bash
set -euo pipefail

sudo cp deploy/systemd/*.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable ms-productos ms-clientes ms-pedidos bff-pedidos360
sudo systemctl restart ms-productos ms-clientes ms-pedidos bff-pedidos360
sudo systemctl status bff-pedidos360 --no-pager
