FROM ubuntu:26.04

RUN apt-get update && apt-get install -y \
    bash \
    build-essential \
    ca-certificates \
    curl \
    git \
    locales \
    xz-utils \
    && rm -rf /var/lib/apt/lists/* \
    && echo "en_US.UTF-8 UTF-8" >> /etc/locale.gen \
    && locale-gen en_US.UTF-8

RUN curl -L https://nixos.org/nix/install | sh -s -- --daemon \
    && mkdir -p /root/.config/nix \
    && echo "experimental-features = nix-command flakes" > /root/.config/nix/nix.conf

RUN groupadd -f nixbld \
    && for i in $(seq 1 10); do useradd -m -g nixbld -G nixbld -s /sbin/nologin "nixbld$i" || true; done

WORKDIR /opt/clj-star-bridge-dev
COPY containers/clj-dev/flake.nix /opt/clj-star-bridge-dev/flake.nix

ENV PATH="/nix/var/nix/profiles/default/bin:/nix/var/nix/profiles/per-user/root/profile/bin:${PATH}" \
    LANG="en_US.UTF-8" \
    LC_ALL="en_US.UTF-8"

RUN . /root/.nix-profile/etc/profile.d/nix.sh \
    && nix flake update --flake /opt/clj-star-bridge-dev \
    && nix develop /opt/clj-star-bridge-dev --command sh -c "java -version && clj --version && node --version && npm --version" || true

RUN mkdir -p /root/.local/bin \
    && printf '%s\n' \
      '#!/usr/bin/env bash' \
      'set -euo pipefail' \
      '. /root/.nix-profile/etc/profile.d/nix.sh' \
      'cd /workspace 2>/dev/null || cd /opt/clj-star-bridge-dev' \
      'exec nix develop /opt/clj-star-bridge-dev --command bash -i' \
      > /root/.local/bin/start-dev-shell.sh \
    && chmod +x /root/.local/bin/start-dev-shell.sh \
    && echo '. /root/.nix-profile/etc/profile.d/nix.sh' >> /root/.bashrc

EXPOSE 8080 3000 9630
ENTRYPOINT ["/root/.local/bin/start-dev-shell.sh"]
