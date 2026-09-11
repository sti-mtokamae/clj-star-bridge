{
  description = "clj-star-bridge development environment";

  inputs = {
    nixpkgs.url = "github:nixos/nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { nixpkgs, flake-utils, ... }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = nixpkgs.legacyPackages.${system};
      in
      {
        devShells.default = pkgs.mkShell {
          packages = with pkgs; [
            clojure
            curl
            git
            jdk21
            nodejs_22
            rlwrap
          ];

          shellHook = ''
            echo "clj-star-bridge development environment loaded"
            echo "  Java: $(java -version 2>&1 | head -1)"
            echo "  Clojure: $(clj --version)"
            echo "  Node: $(node --version)"
            echo "  npm: $(npm --version)"
          '';
        };
      });
}
