# カスタム鉱石 実装ガイド（NoteBlock限定）

このドキュメントでは、このリポジトリで Oraxen の `noteblock` メカニクスを使って、
カスタム鉱石を実装・調整する手順を説明します。

## 対象範囲

- 対象バージョン: Minecraft 1.21.8（`v1_21_R5`）
- ブロック実装方式: `noteblock` のみ
- 目的: カスタムテクスチャ鉱石を、採掘挙動を含めて設定駆動で運用する

## 現在のベースファイル

鉱石テンプレートは次のファイルに定義しています。

- `core/src/main/resources/items/note_block_ores.yml`

現時点のサンプルID:

- `mythril_fragment`（ドロップ用アイテム）
- `mythril_ore`（鉱石ブロック本体）

## 1) リソースパックのテクスチャを追加/差し替え

例として、以下へ画像を配置します。

- `core/src/main/resources/pack/textures/default/mythril_ore.png`
- `core/src/main/resources/pack/textures/default/mythril_fragment.png`

その後、`note_block_ores.yml` の参照を更新します。

- `mythril_ore.Pack.textures`
- `mythril_fragment.Pack.textures`

例:

```yaml
mythril_fragment:
  Pack:
    textures:
      - default/mythril_fragment.png

mythril_ore:
  Pack:
    textures:
      - default/mythril_ore
```

補足:

- ブロックテクスチャは拡張子を省略しても動作する構成が多いです。
- テクスチャ名と `model` 名は整合を取ってください。

## 2) カスタム鉱石を定義（アイテム + ブロック）

`core/src/main/resources/items/note_block_ores.yml` で定義します。

重要部分:

```yaml
mythril_ore:
  material: PAPER
  Pack:
    generate_model: true
    parent_model: block/cube_all
    textures:
      - default/mythril_ore
  Mechanics:
    noteblock:
      custom_variation: 40
      model: mythril_ore
      hardness: 7
      drop:
        silktouch: true
        fortune: true
        minimal_type: IRON
        best_tools:
          - PICKAXE
        loots:
          - oraxen_item: mythril_fragment
            probability: 1.0
```

## 3) 採掘挙動の調整（設定のみで可能）

主な調整項目（コード変更不要）:

- `hardness`: 採掘速度（値が大きいほど遅い）
- `drop.minimal_type`: 最低ツールTier（`WOODEN`, `STONE`, `IRON`, `DIAMOND`, `NETHERITE`）
- `drop.best_tools`: 有効ツール種（鉱石なら通常 `PICKAXE`）
- `drop.silktouch`: シルクタッチ挙動
- `drop.fortune`: 幸運挙動
- `drop.loots`: ドロップ内容と確率
- `blast_resistant`, `immovable`, `can_ignite`, `is_falling`: 物理/環境挙動

推奨の初期値（鉱石）:

- `hardness: 6-8`
- `minimal_type: IRON`
- `best_tools: [PICKAXE]`
- `silktouch: true`
- `fortune: true`

## 4) `custom_variation` 管理（重要）

`noteblock` の variation は重複すると衝突します。

既存サンプルで使われている値:

- `0-6`（`blocks.yml`, `crystalmush.yml` など）

現在の鉱石テンプレート:

- `custom_variation: 40`

推奨運用:

- 鉱石専用レンジを予約（例: `40-99`）
- 追加時にこのREADMEへ管理表を残す

例:

- `40`: `mythril_ore`
- `41`: 予約
- `42`: 予約

## 5) メカニクス有効化（調整済み）

`core/src/main/resources/mechanics.yml` は note_block 中心の運用に合わせてあります。

本方式で確認すべき点:

- `noteblock.enabled: true`
- strict運用にする場合は他カスタムブロックメカニクスを無効化

## 6) 自然生成との連携

このREADMEは鉱石定義と採掘挙動の調整を主眼にしています。

自然生成側（例: Iris）では、Oraxen のブロックIDを参照して配置します。

- `mythril_ore`

生成設定が正しければ、ワールド上ではOraxen側の配置処理を通して
カスタムテクスチャ鉱石として表示されます。

## 7) 新しい鉱石を素早く追加する手順

1. `mythril_fragment` をコピーして新しいドロップアイテムIDを作る
2. `mythril_ore` セクションをコピーしてIDを変更する
3. 重複しない `custom_variation` を割り当てる
4. `Pack.textures` を新しい画像へ差し替える
5. `hardness` と `drop` を調整する
6. 自然生成設定に新IDを登録する

## 8) トラブルシュート

### テクスチャが想定と違う

- `Pack.textures` のパスを確認
- `model` 名の一致を確認
- `custom_variation` の衝突を確認

### 採掘速度が速すぎる/遅すぎる

- `hardness` を調整
- `minimal_type` と `best_tools` の設定を確認

### ドロップが想定と違う

- `drop` セクションの構造を確認
- 参照しているアイテムID（例: `mythril_fragment`）が存在するか確認

## 9) 次の推奨ステップ

まず1種類の鉱石で見た目と採掘バランスを固め、
次に別の `custom_variation` で2種類目を追加して体感比較してから横展開するのがおすすめです。
