# ucoachu-plugin

Ucoachu Features

## Install

```bash
npm install ucoachu-plugin
npx cap sync
```

### Android
  In the project's manifest file, please add two activities.

  ```bash
    <activity
        android:name="com.ucoachu.capacitor.activities.CameraActivity"
        android:exported="true">

    </activity>

    <activity
        android:name="com.ucoachu.capacitor.activities.PlayerActivity"
        android:exported="true">

    </activity>
  ```

## API

<docgen-index>

* [`echo(...)`](#echo)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

### echo(...)

```typescript
echo(options: { value: string; }) => Promise<{ value: string; }>
```

| Param         | Type                            |
| ------------- | ------------------------------- |
| **`options`** | <code>{ value: string; }</code> |

**Returns:** <code>Promise&lt;{ value: string; }&gt;</code>

--------------------

</docgen-api>
