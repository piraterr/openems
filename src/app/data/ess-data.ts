interface EssDataArguments {
  soc: number;
  activePower: number;
  reactivePower: number;
  apparentPower: number;
  gridMode: number;
}

export class EssData {
  soc: number;
  activePower: number;
  reactivePower: number;
  apparentPower: number;
  gridMode: number;

  constructor(args: EssDataArguments) {
    this.soc = args.soc;
    this.activePower = args.activePower;
    this.reactivePower = args.reactivePower;
    this.apparentPower = args.apparentPower;
    this.gridMode = args.gridMode;
  }
}