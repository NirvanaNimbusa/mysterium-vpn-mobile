import { Root as RootBase } from 'native-base'
import * as React from 'react'
import { FavoritesStorage } from '../libraries/favorites-storage'
import TequilApiDriver from '../libraries/tequil-api/tequil-api-driver'
import TequilApiState from '../libraries/tequil-api/tequil-api-state'
import App from './app'
import ErrorDisplayDelegate from './errors/error-display-delegate'
import Logger from './logger'
import VpnAppState from './vpn-app-state'

class Root extends React.PureComponent {
  private readonly tequilApiState = new TequilApiState()
  private readonly vpnAppState = new VpnAppState()
  private errorDisplayDelegate = new ErrorDisplayDelegate()
  private readonly tequilAPIDriver = new TequilApiDriver(this.tequilApiState, this.errorDisplayDelegate)
  private readonly favoritesStore = new FavoritesStorage()

  public async componentWillMount () {
    const logger = new Logger(this.tequilApiState, this.vpnAppState)
    logger.logObservableChanges()
    await this.favoritesStore.fetch()
  }

  public render () {
    return (
      <RootBase>
        <App
          tequilAPIDriver={this.tequilAPIDriver}
          tequilApiState={this.tequilApiState}
          vpnAppState={this.vpnAppState}
          errorDisplayDelegate={this.errorDisplayDelegate}
          favoritesStore={this.favoritesStore}
        />
      </RootBase>
    )
  }
}

export default Root
